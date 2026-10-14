# Despliegue: backend en Render (Docker) y frontend en Vercel

Orden: 1) backend en Render, 2) frontend en Vercel, 3) volver a Render a permitir el dominio de Vercel.

## 1. Backend en Render
1. En Supabase: **Connect → Session pooler** y copia host, usuario y puerto. Render no alcanza la conexión directa
   (`db.xxx.supabase.co`, solo IPv6); usa el pooler en modo sesión (puerto 5432).
   - `DB_URL=jdbc:postgresql://<host-del-pooler>:5432/postgres?sslmode=require`
   - `DB_USER=postgres.<ref-del-proyecto>`
2. En render.com: **New → Web Service** → conecta el repositorio de GitHub.
   - Root Directory: `backend`
   - Runtime / Language: **Docker** (usa `backend/Dockerfile`)
   - Branch: `main`
   - Plan: Free (se duerme tras ~15 min sin uso; la primera petición tarda ~1 min)
3. **Environment** (variables):

| Variable | Valor |
|---|---|
| `DB_URL` | la del pooler (paso 1) |
| `DB_USER` | `postgres.<ref>` |
| `DB_PASSWORD` | contraseña de la base de datos |
| `HCG_JWT_SECRET` | 32+ caracteres aleatorios |
| `HCG_CORS_ORIGINS` | por ahora `http://localhost:5173` (se completa en el paso 3) |
| `HCG_ADMIN_USERNAME` / `HCG_ADMIN_EMAIL` / `HCG_ADMIN_PASSWORD` | solo se usan si la tabla de usuarios está vacía |
| `APISPERU_TOKEN` | token de la empresa |
| `APISPERU_COMPANY_RUC` | `20026788123` |
| `APISPERU_COMPANY_RAZON_SOCIAL` | `MULTISERVICIOS HCG E.I.R.L.` |

4. **Create Web Service**. Cuando diga *Live*, abre `https://<tu-servicio>.onrender.com/swagger-ui.html` para verificar.
   Anota la URL.

## 2. Frontend en Vercel
1. En vercel.com: **Add New → Project** → importa el repositorio.
   - Root Directory: `frontend`
   - Framework: Vite (detectado). Build: `npm run build`. Output: `dist`
2. **Environment Variables**: `VITE_API_URL` = `https://<tu-servicio>.onrender.com` (sin `/` al final).
3. **Deploy**. Anota la URL (`https://<proyecto>.vercel.app`). `frontend/vercel.json` hace que las rutas
   (`/dashboard`, etc.) no den 404 al recargar.

## 3. Permitir el frontend en el backend (CORS)
En Render → Environment: `HCG_CORS_ORIGINS=https://<proyecto>.vercel.app,http://localhost:5173` → guarda (se redespliega).

## Probar con Docker en tu PC (opcional)
```powershell
cd backend
docker build -t hcg-backend .
docker run --rm -p 8080:8080 --env-file ..\.env hcg-backend
```

## Problemas comunes
- **No conecta a la base de datos en Render**: estás usando la conexión directa; usa el Session pooler.
- **Error de CORS en el navegador**: falta el dominio exacto de Vercel en `HCG_CORS_ORIGINS` (sin `/` final).
- **Login lento la primera vez**: el plan gratuito de Render estaba dormido.
- Después de cambiar variables en Vercel hay que **redeploy** (se aplican al compilar).
