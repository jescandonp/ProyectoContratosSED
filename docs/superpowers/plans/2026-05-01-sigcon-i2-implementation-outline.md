# SIGCON Incremento 2 Implementation Outline

> **Tipo de artefacto:** Outline SDD previo a plan ejecutable.  
> **No ejecutar directamente:** este documento debe convertirse en `*-implementation-plan.md` despues de cerrar I1.  
> **Spec fuente:** `docs/superpowers/specs/2026-05-01-sigcon-i2-spec.md`.

## Objetivo

Implementar el nucleo operativo de informes: creacion por contratista, actividades por obligacion, soportes, documentos adicionales, revision, devolucion, aprobacion final y maquina de estados.

## Criterios De Entrada

Antes de convertir este outline en plan ejecutable:

- I1 esta implementado y verificado en `local-dev`.
- Existen backend, frontend, `db/00_setup.sql`, `db/01_datos_prueba.sql` y `ARRANQUE.md`.
- `DocumentStorageService` existe y almacena firma en `local-dev`.
- Detalle de contrato muestra historial vacio y boton "Nuevo Informe" deshabilitado sin llamadas a `/api/informes`.
- Usuarios, roles, contratos, obligaciones, catalogo OPS y asignaciones funcionan.
- Swagger y health estan operativos.

## Macro-Tareas Previstas

1. **Migracion y datos I2**
   - Agregar `SGCN_INFORMES`, `SGCN_ACTIVIDADES`, `SGCN_SOPORTES`, `SGCN_DOCS_ADICIONALES`, `SGCN_OBSERVACIONES`.
   - Agregar datos de prueba para informe borrador y soportes.

2. **Dominio backend**
   - Crear entidades, enums y repositorios I2.
   - Mantener auditoria, borrado logico y relaciones contra entidades I1.

3. **Servicios de informe**
   - Crear `InformeService`, `ActividadInformeService`, `SoporteAdjuntoService`, `DocumentoAdicionalInformeService`, `ObservacionService`, `InformeEstadoService`.
   - Reusar `DocumentStorageService` para soportes de archivo.

4. **Maquina de estados y seguridad**
   - Implementar transiciones `BORRADOR`, `ENVIADO`, `EN_REVISION`, `DEVUELTO`, `APROBADO`.
   - Validar rol y pertenencia al contrato en cada accion.
   - Rechazar transiciones invalidas.

5. **APIs I2**
   - Agregar controllers `/api/informes`, `/api/informes/{id}/actividades`, `/api/actividades/{id}/soportes`, `/api/informes/{id}/documentos-adicionales`.
   - Documentar todo en Swagger.

6. **Frontend I2**
   - Habilitar "Nuevo Informe".
   - Crear formulario, previsualizacion, correccion, detalle de informe, cola de revision y cola de aprobacion.
   - Usar prototipos `nuevo_informe_de_actividades_optimizado_sigcon`, `corregir_informe_devuelto_optimizado_sigcon` y `cola_de_revisi_n_de_informes_sigcon`.

7. **Pruebas y verificacion**
   - Pruebas de servicios para transiciones.
   - Pruebas de controller para permisos por rol.
   - Pruebas frontend de guards, formularios y colas.
   - Flujo manual completo: borrador -> envio -> devolucion -> correccion -> revision -> aprobacion.

## Riesgos Y Decisiones A Revisar Tras I1

- Si `DocumentStorageService` queda demasiado acoplado a firma, redisenarlo antes de soportes.
- Si los DTOs de contrato no incluyen suficientes datos de asignacion, extenderlos antes de colas.
- Si el frontend shell no soporta navegacion por rol para revisor/supervisor, ajustarlo antes de pantallas I2.
- Si `db/00_setup.sql` queda monolitico, decidir si se mantiene un archivo unico como pide PRD o se agregan secciones claramente marcadas por incremento.
- Si Oracle local no esta disponible, definir estrategia de pruebas backend con perfil test sin romper compatibilidad Oracle.

## Criterios De Salida

- Flujo completo I2 en `local-dev`.
- No hay PDF real ni notificaciones.
- Informe `APROBADO` puede existir sin `pdfRuta`.
- I1 sigue pasando sus criterios.
- Swagger documenta endpoints I2.
- `ARRANQUE.md` queda actualizado con flujo I2.

## Promocion A Plan Ejecutable

Convertir este outline en `docs/superpowers/plans/YYYY-MM-DD-sigcon-i2-implementation-plan.md` despues de:

- Revisar diff final de I1.
- Confirmar nombres reales de paquetes, servicios, DTOs y rutas Angular.
- Ajustar tareas al patron de pruebas realmente usado en I1.
- Revalidar la spec I2 contra cualquier cambio aprobado durante I1.
