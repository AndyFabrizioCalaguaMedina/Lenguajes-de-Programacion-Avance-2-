import { BrowserRouter, Routes, Route } from "react-router-dom";
import MainLayout from "./components/layout/MainLayout";
import DashboardPage from "./features/dashboard/DashboardPage";
import CotizacionesPage from "./features/cotizaciones/CotizacionesPage";
import NuevaCotizacion from "./features/cotizaciones/nuevaCotizacion/NuevaCotizacion";
import DocumentosPage from "./features/documentos/DocumentosPage";
import BoletasPage from "./features/boletas/BoletasPage";
import BodegaPage from "./features/bodega/BodegaPage";
import BusquedaPage from "./features/busqueda/BusquedaPage";
import ReportesPage from "./features/reportes/ReportesPage";
import UsuariosPage from "./features/usuarios/UsuariosPage";
import HistorialPage from "./features/historial/HistorialPage";
import CatalogoPage from "./features/catalogo/CatalogoPage";
import { PRODUCTOS, PROVEEDORES } from "./features/catalogo/configs";
import OperacionesPage from "./features/operaciones/OperacionesPage";
import DetalleOperacionPage from "./features/operaciones/DetalleOperacionPage";
import ClientesPage from "./features/clientes/ClientesPage";
import LoginPage from "./features/auth/LoginPage";
import RutaProtegida from "./features/auth/RutaProtegida";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LoginPage />} />

        <Route element={<RutaProtegida />}>
          <Route element={<MainLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/operaciones" element={<OperacionesPage />} />
            <Route path="/operaciones/:id" element={<DetalleOperacionPage />} />
            <Route path="/clientes" element={<ClientesPage />} />
            <Route path="/cotizaciones" element={<CotizacionesPage />} />
            <Route path="/cotizaciones/nueva-cotizacion" element={<NuevaCotizacion />} />
            <Route path="/documentos" element={<DocumentosPage />} />
            <Route path="/boletas" element={<BoletasPage />} />
            <Route path="/bodega" element={<BodegaPage />} />
            <Route path="/productos" element={<CatalogoPage key="prod" config={PRODUCTOS} />} />
            <Route path="/proveedores" element={<CatalogoPage key="prov" config={PROVEEDORES} />} />
            <Route path="/busqueda" element={<BusquedaPage />} />
            <Route path="/reportes" element={<ReportesPage />} />
            <Route path="/usuarios" element={<UsuariosPage />} />
            <Route path="/historial" element={<HistorialPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}