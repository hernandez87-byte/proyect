#!/usr/bin/env python3
"""Sistema de calendario de numerología.

Genera ciclos personales (año, mes y día) a partir de una fecha de nacimiento
utilizando reducción pitagórica (1-9, con conservación de 11 y 22 para camino de vida).
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, timedelta
import argparse
import calendar

MASTER_NUMBERS = {11, 22}


def suma_digitos(valor: int) -> int:
    return sum(int(d) for d in str(abs(valor)))


def reducir_numerologia(valor: int, *, conservar_maestros: bool = False) -> int:
    actual = abs(valor)
    while actual > 9:
        if conservar_maestros and actual in MASTER_NUMBERS:
            return actual
        actual = suma_digitos(actual)
    return actual


@dataclass(frozen=True)
class PerfilNumerologico:
    nacimiento: date
    camino_vida: int

    @staticmethod
    def crear(nacimiento: date) -> "PerfilNumerologico":
        base = int(nacimiento.strftime("%Y%m%d"))
        camino = reducir_numerologia(base, conservar_maestros=True)
        return PerfilNumerologico(nacimiento=nacimiento, camino_vida=camino)


@dataclass(frozen=True)
class CicloPersonal:
    fecha: date
    anio_personal: int
    mes_personal: int
    dia_personal: int


def calcular_anio_personal(nacimiento: date, anio_objetivo: int) -> int:
    valor = nacimiento.day + nacimiento.month + anio_objetivo
    return reducir_numerologia(valor)


def calcular_mes_personal(anio_personal: int, mes: int) -> int:
    return reducir_numerologia(anio_personal + mes)


def calcular_dia_personal(mes_personal: int, dia: int) -> int:
    return reducir_numerologia(mes_personal + dia)


def generar_calendario_personal(nacimiento: date, anio_objetivo: int, mes_objetivo: int | None = None) -> list[CicloPersonal]:
    anio_personal = calcular_anio_personal(nacimiento, anio_objetivo)
    meses = [mes_objetivo] if mes_objetivo else list(range(1, 13))
    resultado: list[CicloPersonal] = []

    for mes in meses:
        _, dias_mes = calendar.monthrange(anio_objetivo, mes)
        mes_personal = calcular_mes_personal(anio_personal, mes)
        for dia in range(1, dias_mes + 1):
            fecha = date(anio_objetivo, mes, dia)
            dia_personal = calcular_dia_personal(mes_personal, dia)
            resultado.append(
                CicloPersonal(
                    fecha=fecha,
                    anio_personal=anio_personal,
                    mes_personal=mes_personal,
                    dia_personal=dia_personal,
                )
            )
    return resultado


def parsear_fecha(texto: str) -> date:
    try:
        return datetime.strptime(texto, "%Y-%m-%d").date()
    except ValueError as exc:
        raise argparse.ArgumentTypeError(
            f"Fecha inválida '{texto}'. Usa formato YYYY-MM-DD"
        ) from exc


def construir_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generador de calendario de numerología personal"
    )
    parser.add_argument(
        "--nacimiento",
        type=parsear_fecha,
        required=True,
        help="Fecha de nacimiento (YYYY-MM-DD)",
    )
    parser.add_argument(
        "--anio",
        type=int,
        default=date.today().year,
        help="Año a calcular (por defecto: año actual)",
    )
    parser.add_argument(
        "--mes",
        type=int,
        choices=range(1, 13),
        help="Mes a calcular (1-12). Si se omite, calcula todo el año",
    )
    parser.add_argument(
        "--limite",
        type=int,
        default=31,
        help="Cantidad máxima de filas a mostrar",
    )
    return parser


def main() -> None:
    parser = construir_parser()
    args = parser.parse_args()

    perfil = PerfilNumerologico.crear(args.nacimiento)
    calendario_personal = generar_calendario_personal(args.nacimiento, args.anio, args.mes)

    print("\n=== SISTEMA DE CALENDARIO DE NUMEROLOGÍA ===")
    print(f"Nacimiento: {perfil.nacimiento.isoformat()}")
    print(f"Camino de vida: {perfil.camino_vida}")
    print(f"Registros generados: {len(calendario_personal)}")
    print("\nFecha       | Año | Mes | Día")
    print("------------+-----+-----+----")

    for ciclo in calendario_personal[: args.limite]:
        print(
            f"{ciclo.fecha.isoformat()} |  {ciclo.anio_personal}  |  {ciclo.mes_personal}  |  {ciclo.dia_personal}"
        )

    if len(calendario_personal) > args.limite:
        print(f"... ({len(calendario_personal) - args.limite} filas más)")


if __name__ == "__main__":
    main()
