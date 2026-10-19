# Opciones de cartas de Deck of Worlds

Auditoría visual de las 240 imágenes WebP incluidas en el paquete. El corte original de las imágenes desplazó algunas cartas al grupo vecino; `scripts/normalize_story_engine_deck.py` corrige el mazo sin cambiar los identificadores ni las rutas de imagen.

| Mazo | Cartas | Opciones por carta | Distribución |
| --- | ---: | ---: | --- |
| Regiones | 32 | 1 | Imagen completa |
| Puntos de interés | 48 | 2 | Superior / inferior |
| Nombres y apelativos | 48 | 4 | Norte / este / sur / oeste |
| Orígenes e historia | 32 | 4 | Norte / este / sur / oeste |
| Atributos y cultura | 48 | 4 | Norte / este / sur / oeste |
| Acontecimientos y ganchos | 32 | 2 | Superior / inferior |

La app muestra la imagen original y permite elegir manualmente o tirar d2/d4 en el visor de la carta. El texto impreso en las imágenes no se transcribió: las zonas de contenido quedan vacías para no introducir resultados OCR erróneos. Se pueden completar individualmente desde el editor de cartas. El selector visual sigue funcionando sin esa transcripción.

El generador de microescenarios utiliza la cantidad de opciones de cada mazo. Al copiar el resultado a las notas registra, como mínimo, la carta y la opción elegida; si la zona tiene texto editado, también lo incluye.

Al importar manualmente un mazo de imágenes, el usuario puede elegir 1, 2 o 4 opciones por carta. La app crea esas zonas vacías y permite completarlas luego. Los ZIP exportados incluyen el modo y los textos de cada zona en `deck_manifest.json` (versión 2); los manifiestos anteriores siguen siendo importables.

En un manifiesto hecho a mano, cada cara puede definir su propio modo y sus zonas, por ejemplo:

```json
{
  "name": "Frente",
  "fileName": "carta_01.webp",
  "contentMode": "TOP_BOTTOM_SPLIT",
  "zones": [{"text": "Opción superior"}, {"text": "Opción inferior"}]
}
```

Para cuatro opciones se usa `FOUR_EDGE_CUES` y las zonas van en orden norte, este, sur y oeste. Para una opción, `IMAGE_WITH_TEXT` contiene una zona. Las caras de un mismo ZIP pueden usar modos distintos.

Al volver a importar el paquete Deck of Worlds sobre una instalación existente, la sincronización compara las rutas de imagen y actualiza las cartas en su lugar para conservar sus IDs, estado de robo y referencias de sesión. Los textos de zona ya editados se conservan.
