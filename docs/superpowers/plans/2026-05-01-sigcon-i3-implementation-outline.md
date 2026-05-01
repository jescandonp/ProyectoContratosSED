# SIGCON Incremento 3 Implementation Outline

> **Tipo de artefacto:** Outline SDD previo a plan ejecutable.  
> **No ejecutar directamente:** este documento debe convertirse en `*-implementation-plan.md` despues de cerrar I2.  
> **Spec fuente:** `docs/superpowers/specs/2026-05-01-sigcon-i3-spec.md`.

## Objetivo

Completar SIGCON para produccion funcional: generacion de PDF institucional con firmas, descarga autorizada, notificaciones in-app y notificaciones por email Office 365.

## Criterios De Entrada

Antes de convertir este outline en plan ejecutable:

- I1 e I2 estan implementados y verificados.
- La maquina de estados de informes funciona sin transiciones invalidas.
- La aprobacion final `EN_REVISION -> APROBADO` esta centralizada en `InformeEstadoService` o equivalente.
- `DocumentStorageService` soporta firma y soportes de archivo.
- Contratista y supervisor pueden cargar `firmaImagen`.
- El detalle de contrato e historial de informes conocen estado `APROBADO`.
- Existen datos suficientes para renderizar un informe aprobado.

## Macro-Tareas Previstas

1. **Migracion y datos I3**
   - Agregar `PDF_GENERADO_AT`, `PDF_HASH` a `SGCN_INFORMES`.
   - Agregar `SGCN_NOTIFICACIONES`.
   - Agregar datos de prueba para notificaciones y PDF cuando aplique.

2. **PDF institucional**
   - Crear `PdfInformeService` e `InformePdfTemplateService`.
   - Basar layout en `Notas_ProyectoContratos/06_Informe_actividades_06_Abril_2026_Juan_Escandon.docx`.
   - Incluir contrato, periodo, actividades, soportes, documentos, firmas y metadata.
   - Calcular hash sobre bytes finales.

3. **Integracion con aprobacion final**
   - En `EN_REVISION -> APROBADO`, generar PDF antes de confirmar estado.
   - Fallar aprobacion con `FIRMA_REQUERIDA` si falta firma de contratista o supervisor.
   - Fallar aprobacion con `PDF_GENERACION_FALLIDA` si no se puede crear o almacenar el PDF.
   - Mantener PDF inmutable despues de aprobado.

4. **Notificaciones in-app**
   - Crear `NotificacionService`.
   - Crear eventos `INFORME_ENVIADO`, `REVISION_APROBADA`, `REVISION_DEVUELTA`, `INFORME_APROBADO`, `INFORME_DEVUELTO`.
   - Agregar endpoints de lista, contador y marcado como leida.

5. **Email Office 365**
   - Crear `EmailNotificacionService`.
   - Simular envios en `local-dev`.
   - Usar Microsoft Graph / Office 365 en `weblogic`.
   - Registrar errores de envio sin revertir aprobacion si el PDF ya fue generado.

6. **Frontend I3**
   - Agregar campana y centro de notificaciones.
   - Agregar visor/descarga PDF.
   - Advertir en perfil cuando falte firma para contratista o supervisor.
   - Usar prototipos `centro_de_notificaciones_sigcon`, `visor_de_reporte_aprobado_pdf_sigcon`, `mi_perfil_y_firma_sigcon`.

7. **Pruebas y verificacion**
   - Pruebas backend de PDF, hash, inmutabilidad y autorizacion.
   - Pruebas backend de notificaciones por destinatario.
   - Pruebas de email simulado en `local-dev`.
   - Pruebas frontend de badge, centro de notificaciones y visor PDF.
   - Flujo manual: aprobar informe -> PDF generado -> notificacion creada -> email simulado/enviado.

## Riesgos Y Decisiones A Revisar Tras I2

- Seleccion tecnica de generacion PDF debe respetar Java 8 y WebLogic 12.
- El template DOCX es referencia visual, no necesariamente motor de generacion.
- Confirmar si Microsoft Graph esta aprobado por infraestructura SED antes de cerrar plan detallado.
- Confirmar ruta de almacenamiento final para PDFs en `weblogic`.
- Confirmar si `PDF_HASH` usa SHA-256 u otro algoritmo institucional.
- Evaluar si se requiere tabla de auditoria de email fuera del MVP antes de implementarla.

## Criterios De Salida

- Aprobar informe genera PDF y metadata.
- PDF incluye firmas y no se regenera.
- Descarga PDF funciona para contratista, supervisor y admin.
- Notificaciones in-app aparecen y se marcan como leidas.
- Email se simula en `local-dev` y queda configurable para `weblogic`.
- I1 e I2 siguen funcionando.
- `ARRANQUE.md` documenta configuracion PDF/email/notificaciones.

## Promocion A Plan Ejecutable

Convertir este outline en `docs/superpowers/plans/YYYY-MM-DD-sigcon-i3-implementation-plan.md` despues de:

- Revisar diff final de I2.
- Confirmar servicios y endpoints reales de informes.
- Validar la estrategia tecnica de PDF con Java 8/WebLogic.
- Validar credenciales y mecanismo Office 365 con el entorno SED.
- Revalidar la spec I3 contra cualquier cambio aprobado durante I2.
