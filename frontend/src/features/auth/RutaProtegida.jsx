import { Navigate, Outlet } from "react-router-dom";
import { tokenVigente } from "../../services/api";

export default function RutaProtegida() {
  if (!tokenVigente()) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}