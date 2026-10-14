package com.hcg.multiservicios.util;

/** Utilidades de texto para normalizar entradas de formularios. */
public final class Texto {

    private Texto() {
    }

    /** Recorta espacios; vacio o solo espacios pasa a null. */
    public static String limpio(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Escapa % _ \ para usar el texto dentro de un LIKE con escape '\'. */
    public static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
