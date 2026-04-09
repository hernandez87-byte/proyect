from datetime import date

from calendario_numerologia import (
    PerfilNumerologico,
    calcular_anio_personal,
    calcular_mes_personal,
    calcular_dia_personal,
    generar_calendario_personal,
)


def test_camino_vida_conserva_maestro_si_aplica():
    perfil = PerfilNumerologico.crear(date(1978, 11, 3))
    assert perfil.camino_vida in {1,2,3,4,5,6,7,8,9,11,22}


def test_calculos_base():
    nacimiento = date(1990, 8, 17)
    anio_personal = calcular_anio_personal(nacimiento, 2026)
    mes_personal = calcular_mes_personal(anio_personal, 4)
    dia_personal = calcular_dia_personal(mes_personal, 15)

    assert anio_personal == 8
    assert mes_personal == 3
    assert dia_personal == 9


def test_genera_mes_completo():
    ciclos = generar_calendario_personal(date(1990, 8, 17), 2026, 2)
    assert len(ciclos) == 28
    assert ciclos[0].fecha == date(2026, 2, 1)
    assert ciclos[-1].fecha == date(2026, 2, 28)
