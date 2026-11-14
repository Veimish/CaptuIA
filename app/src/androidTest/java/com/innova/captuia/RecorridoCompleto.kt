package com.innova.captuia

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Recorrido completo de la app en orden: cada etapa continúa desde el estado
 * que deja la anterior, como lo haría un usuario real.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    Etapa0PermisosTest::class,
    Etapa1GaleriaTest::class,
    Etapa2TextoYBusquedaTest::class,
    Etapa3CarpetasTest::class,
    Etapa4HorarioTest::class,
)
class RecorridoCompleto
