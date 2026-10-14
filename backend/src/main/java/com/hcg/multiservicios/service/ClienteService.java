package com.hcg.multiservicios.service;

import com.hcg.multiservicios.dto.PageResponse;
import com.hcg.multiservicios.dto.catalogo.ClienteDto;
import com.hcg.multiservicios.dto.catalogo.ClienteRequest;
import com.hcg.multiservicios.entity.Cliente;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.exception.FieldValidationException;
import com.hcg.multiservicios.repository.ClienteRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.util.Texto;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final AuditoriaService auditoria;

    public ClienteService(ClienteRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    /** Busca por codigo, RUC, DNI, nombre, razon social, nombre comercial, nombres o apellidos. */
    @Transactional(readOnly = true)
    public PageResponse<ClienteDto> buscar(String q, Boolean activo, int page, int size) {
        String texto = Texto.limpio(q);
        Specification<Cliente> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (texto != null) {
                String p = "%" + Texto.escapeLike(texto.toLowerCase(Locale.ROOT)) + "%";
                String[] campos = {"codigo", "nombre", "ruc", "dni", "razonSocial", "nombreComercial", "nombres", "apellidos"};
                Predicate[] ors = new Predicate[campos.length];
                for (int i = 0; i < campos.length; i++) {
                    ors[i] = cb.like(cb.lower(root.<String>get(campos[i])), p, '\\');
                }
                ps.add(cb.or(ors));
            }
            if (activo != null) {
                ps.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("nombre").ascending().and(Sort.by("id")));
        return PageResponse.of(repository.findAll(spec, pageable).map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ClienteDto obtener(Integer id) {
        return toDto(buscarPorId(id));
    }

    @Transactional
    public ClienteDto crear(ClienteRequest req, UsuarioPrincipal actor) {
        validarReglas(req);
        Cliente c = new Cliente();
        aplicar(c, req);
        verificarDuplicados(c, null);
        c = repository.save(c);
        c.setCodigo(String.format("CLI-%06d", c.getId()));
        repository.save(c);
        auditoria.registrar(actor, "CLIENTE_CREADO", "Cliente", c.getId(), null, resumen(c));
        return toDto(c);
    }

    @Transactional
    public ClienteDto actualizar(Integer id, ClienteRequest req, UsuarioPrincipal actor) {
        validarReglas(req);
        Cliente c = buscarPorId(id);
        String antes = resumen(c);
        aplicar(c, req);
        verificarDuplicados(c, id);
        repository.save(c);
        auditoria.registrar(actor, "CLIENTE_EDITADO", "Cliente", id, antes, resumen(c));
        return toDto(c);
    }

    @Transactional
    public ClienteDto cambiarEstado(Integer id, boolean activo, UsuarioPrincipal actor) {
        Cliente c = buscarPorId(id);
        boolean antes = c.isActivo();
        c.setActivo(activo);
        repository.save(c);
        auditoria.registrar(actor, activo ? "CLIENTE_ACTIVADO" : "CLIENTE_DESACTIVADO", "Cliente", id,
                "activo=" + antes, "activo=" + activo);
        return toDto(c);
    }

    /** Elimina de verdad solo si el cliente no tiene cotizaciones, operaciones ni salidas; si las tiene, se debe desactivar. */
    @Transactional
    public void eliminar(Integer id, UsuarioPrincipal actor) {
        Cliente c = buscarPorId(id);
        String antes = resumen(c);
        try {
            repository.delete(c);
            repository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw BusinessException.conflict("CLIENT_IN_USE",
                    "No se puede eliminar: el cliente tiene cotizaciones, operaciones o salidas asociadas. Desactívalo en su lugar.");
        }
        auditoria.registrar(actor, "CLIENTE_ELIMINADO", "Cliente", id, antes, null);
    }

    private Cliente buscarPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CLIENT_NOT_FOUND", "Cliente no encontrado."));
    }

    /** Reglas entre campos que dependen del tipo de documento. */
    private void validarReglas(ClienteRequest r) {
        Map<String, String> errores = new LinkedHashMap<>();
        String ruc = Texto.limpio(r.ruc());
        String dni = Texto.limpio(r.dni());
        String razon = Texto.limpio(r.razonSocial());
        String nombres = Texto.limpio(r.nombres());
        String apellidos = Texto.limpio(r.apellidos());
        switch (r.tipoDocumento()) {
            case Cliente.TIPO_RUC -> {
                if (ruc == null) {
                    errores.put("ruc", "El RUC es obligatorio para este tipo de cliente");
                }
                if (razon == null) {
                    errores.put("razonSocial", "La razón social es obligatoria para clientes con RUC");
                }
            }
            case Cliente.TIPO_DNI -> {
                if (dni == null) {
                    errores.put("dni", "El DNI es obligatorio para este tipo de cliente");
                }
                if (nombres == null) {
                    errores.put("nombres", "Los nombres son obligatorios para clientes con DNI");
                }
                if (apellidos == null) {
                    errores.put("apellidos", "Los apellidos son obligatorios para clientes con DNI");
                }
            }
            default -> {
                if (razon == null && nombres == null) {
                    errores.put("nombres", "Indique razón social o nombres");
                }
            }
        }
        if (!errores.isEmpty()) {
            throw new FieldValidationException(errores);
        }
    }

    private void aplicar(Cliente c, ClienteRequest r) {
        c.setTipoDocumento(r.tipoDocumento());
        c.setRuc(Texto.limpio(r.ruc()));
        c.setDni(Texto.limpio(r.dni()));
        c.setRazonSocial(Texto.limpio(r.razonSocial()));
        c.setNombreComercial(Texto.limpio(r.nombreComercial()));
        c.setNombres(Texto.limpio(r.nombres()));
        c.setApellidos(Texto.limpio(r.apellidos()));
        c.setDireccion(Texto.limpio(r.direccion()));
        c.setTelefono(Texto.limpio(r.telefono()));
        String correo = Texto.limpio(r.correo());
        c.setCorreo(correo == null ? null : correo.toLowerCase(Locale.ROOT));
        c.setNombre(nombreParaMostrar(c));
    }

    private String nombreParaMostrar(Cliente c) {
        if (c.getRazonSocial() != null) {
            return c.getRazonSocial();
        }
        if (c.getNombres() != null) {
            return c.getApellidos() != null ? c.getNombres() + " " + c.getApellidos() : c.getNombres();
        }
        return c.getNombreComercial();
    }

    private void verificarDuplicados(Cliente c, Integer idActual) {
        if (c.getRuc() != null) {
            boolean existe = idActual == null ? repository.existsByRuc(c.getRuc())
                    : repository.existsByRucAndIdNot(c.getRuc(), idActual);
            if (existe) {
                throw BusinessException.conflict("DUPLICATE_RUC", "Ya existe un cliente con ese RUC.");
            }
        }
        if (c.getDni() != null) {
            boolean existe = idActual == null ? repository.existsByDni(c.getDni())
                    : repository.existsByDniAndIdNot(c.getDni(), idActual);
            if (existe) {
                throw BusinessException.conflict("DUPLICATE_DNI", "Ya existe un cliente con ese DNI.");
            }
        }
    }

    private String resumen(Cliente c) {
        return "codigo=" + c.getCodigo() + ", nombre=" + c.getNombre() + ", ruc=" + c.getRuc() + ", dni=" + c.getDni();
    }

    private ClienteDto toDto(Cliente c) {
        String numero = Cliente.TIPO_RUC.equals(c.getTipoDocumento()) ? c.getRuc()
                : Cliente.TIPO_DNI.equals(c.getTipoDocumento()) ? c.getDni() : null;
        return new ClienteDto(c.getId(), c.getCodigo(), c.getTipoDocumento(), numero, c.getRuc(), c.getDni(),
                c.getRazonSocial(), c.getNombreComercial(), c.getNombres(), c.getApellidos(), c.getNombre(),
                c.getDireccion(), c.getTelefono(), c.getCorreo(), c.isActivo(), c.getFechaRegistro());
    }
}
