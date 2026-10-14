package com.hcg.multiservicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hcg.multiservicios.dto.CotizacionRequest;
import com.hcg.multiservicios.entity.Documento;
import com.hcg.multiservicios.exception.BusinessException;
import com.hcg.multiservicios.repository.AuditoriaRepository;
import com.hcg.multiservicios.repository.CotizacionRepository;
import com.hcg.multiservicios.repository.DocumentoRepository;
import com.hcg.multiservicios.security.UsuarioPrincipal;
import com.hcg.multiservicios.service.CotizacionService;
import com.hcg.multiservicios.service.DocumentoService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Prueba los limites transaccionales reales sin una transaccion global de test. */
@SpringBootTest
@ActiveProfiles("test")
class BackendAuditIntegrationTest {
    @Autowired CotizacionService cotizaciones;
    @Autowired DocumentoService documentos;
    @Autowired CotizacionRepository cotizacionRepository;
    @Autowired DocumentoRepository documentoRepository;
    @Autowired AuditoriaRepository auditoriaRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager em;

    private CotizacionRequest request(String precio) {
        return new CotizacionRequest(null, "Cliente auditoria", null, null, null,
                List.of(new CotizacionRequest.Item(null, "Servicio", "ZZ",
                        new BigDecimal("2.00"), new BigDecimal(precio))));
    }

    @Test
    void usuarioNuloPersisteYActualizacionMantieneUnSoloDocumento() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var c = cotizaciones.crear(request("59.00"), null);
            em.flush();
            em.clear();
            var inicial = documentoRepository.findByTipoAndNumero("COTIZACION", c.numero()).orElseThrow();
            Long docId = inicial.getId();
            var creadoEn = inicial.getCreadoEn();
            assertThat(c.id()).isPositive();
            assertThat(inicial.getUsuarioId()).isNull();
            assertThat(inicial.getOrigenId()).isEqualTo(c.id().toString());
            assertThat(inicial.getMonto()).isEqualByComparingTo("118.00");
            assertThat(creadoEn).isNotNull();
            cotizaciones.actualizar(c.id(), request("118.00"), null);
            em.flush();
            em.clear();
            var actualizado = documentoRepository.findByTipoAndNumero("COTIZACION", c.numero()).orElseThrow();
            assertThat(actualizado.getId()).isEqualTo(docId);
            assertThat(actualizado.getCreadoEn()).isEqualTo(creadoEn);
            assertThat(actualizado.getMonto()).isEqualByComparingTo("236.00");
            assertThat(cotizaciones.obtener(c.id()).total()).isEqualByComparingTo("236.00");
            status.setRollbackOnly();
        });
    }

    @Test
    void foreignKeyInvalidaRevierteCotizacionDocumentoYAuditoria() {
        long cotCount = cotizacionRepository.count();
        long docCount = documentoRepository.count();
        long auditCount = auditoriaRepository.count();
        assertThatThrownBy(() -> cotizaciones.crear(request("59.00"),
                new UsuarioPrincipal(Long.MAX_VALUE, "usuario_inexistente", "Usuario inexistente")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(cotizacionRepository.count()).isEqualTo(cotCount);
        assertThat(documentoRepository.count()).isEqualTo(docCount);
        assertThat(auditoriaRepository.count()).isEqualTo(auditCount);
    }

    @Test
    void documentoAutomaticoNoSobrescribeOrigenManual() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Documento manual = new Documento();
            manual.setTipo("BOLETA");
            manual.setNumero("AUDIT-ORIGEN");
            manual.setFecha(LocalDate.of(2026, 10, 8));
            documentoRepository.saveAndFlush(manual);
            assertThatThrownBy(() -> documentos.registrarAutomatico("BOLETA", "AUDIT-ORIGEN",
                    manual.getFecha(), Documento.ORIGEN_COMPROBANTE, "999", "Cliente", null,
                    BigDecimal.TEN, "PEN", "PENDIENTE", null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("otro origen");
            assertThat(manual.getOrigen()).isEqualTo(Documento.ORIGEN_MANUAL);
            status.setRollbackOnly();
        });
    }
}
