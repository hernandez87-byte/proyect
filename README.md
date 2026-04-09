# Sistema de Calendario de Numerología

Script en Python para generar un calendario numerológico personal:

- **Camino de vida** (desde fecha de nacimiento).
- **Año personal**.
- **Mes personal**.
- **Día personal**.

## Uso

```bash
python3 calendario_numerologia.py --nacimiento 1990-08-17 --anio 2026 --mes 4
```

Opciones:

- `--nacimiento` (obligatorio): fecha en formato `YYYY-MM-DD`.
- `--anio` (opcional): año a evaluar, por defecto el año actual.
- `--mes` (opcional): si se indica, calcula solo ese mes; si no, calcula todo el año.
- `--limite` (opcional): máximo de filas mostradas en consola.

## Ejemplo de salida

```text
=== SISTEMA DE CALENDARIO DE NUMEROLOGÍA ===
Nacimiento: 1990-08-17
Camino de vida: 8
Registros generados: 30

Fecha       | Año | Mes | Día
------------+-----+-----+----
2026-04-01 |  8  |  3  |  4
...
```
