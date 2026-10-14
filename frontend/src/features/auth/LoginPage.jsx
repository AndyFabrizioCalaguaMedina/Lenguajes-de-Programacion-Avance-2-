import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../../services/api";

export default function LoginPage() {
  const [identificador, setIdentificador] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setCargando(true);
    try {
      await login(identificador, password);
      navigate("/dashboard");
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-gradient-to-br from-hcg-navy-dark via-hcg-navy to-hcg-navy-soft px-4">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm bg-white rounded-2xl shadow-xl border-t-4 border-hcg-gold p-8 space-y-5"
      >
        <img src="/logo-hcg.png" alt="HCG Multiservicios" className="mx-auto h-14 w-auto" />
        <div className="text-center">
          <h1 className="text-slate-800 text-xl font-bold">Iniciar sesión</h1>
          <p className="text-slate-500 text-sm">Gestión documental y comercial</p>
        </div>

        <label className="block text-sm text-slate-700">
          Usuario o correo
          <input
            type="text"
            value={identificador}
            onChange={(e) => setIdentificador(e.target.value)}
            required
            autoFocus
            className="mt-1 w-full rounded-lg border border-slate-300 p-2"
          />
        </label>

        <label className="block text-sm text-slate-700">
          Contraseña
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="mt-1 w-full rounded-lg border border-slate-300 p-2"
          />
        </label>

        {error && (
          <p role="alert" className="text-red-600 text-sm">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={cargando}
          className="w-full bg-hcg-navy hover:bg-hcg-navy-soft text-white font-semibold rounded-lg p-2.5 disabled:opacity-60"
        >
          {cargando ? "Entrando..." : "Entrar"}
        </button>
      </form>
    </main>
  );
}