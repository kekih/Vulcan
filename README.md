# Vulcan — Плагин вулканов для Paper 1.16.5

Полноценная система автоматических вулканов в игровом мире.

## Скачать готовый jar

1. Открой **[Actions](https://github.com/kekih/Vulcan/actions)**
2. Выбери последний успешный workflow **Build Vulcan**
3. Внизу страницы скачай артефакт **Vulcan** (zip с `Vulcan.jar`)

Или создай **Release** (Releases → Draft a new release / tag `v1.0.0`) — jar прикрепится автоматически.

Сборка запускается автоматически при каждом пуше в `main`.

## Особенности

- **Три типа вулканов**: Обычный, Легендарный (самый ценный), Мифический (самый редкий)
- Автоматическое появление каждые **4 часа**
- Физическая структура вулкана в мире (конус blackstone/basalt/magma + кратер с лавой)
- 6 минут до извержения + эффекты
- Предметы физически выбрасываются в мир
- Награды из руки: `/vulcan add <тип>`
- Таймеры сохраняются после рестарта
- Только `/vulcan info` для игроков
- Все сообщения на русском

## Установка

1. Скачай `Vulcan.jar` (см. выше)
2. Положи в `plugins` на Paper 1.16.5
3. Перезапусти сервер
4. Настрой точки появления:
   ```
   /vulcan setlocation normal
   /vulcan setlocation legendary
   /vulcan setlocation mythical
   ```
5. Добавь лут (предмет в руке):
   ```
   /vulcan add normal
   ```
6. Шансы: `/vulcan setchance normal 0 25`

## Команды

### Игроки
| Команда | Описание |
|---------|----------|
| `/vulcan info` | Инфо о текущем / следующем вулкане |

### Админы (`vulcan.admin`)
| Команда | Описание |
|---------|----------|
| `/vulcan spawn <тип>` | Вызвать вулкан |
| `/vulcan despawn` | Удалить активный |
| `/vulcan setlocation <тип>` | Сохранить координаты |
| `/vulcan setinterval <сек>` | Интервал появления |
| `/vulcan setduration <сек>` | Время до извержения |
| `/vulcan add <тип>` | Добавить предмет из руки |
| `/vulcan remove <тип> <номер>` | Удалить награду |
| `/vulcan setchance <тип> <номер> <шанс>` | Шанс награды |
| `/vulcan check <тип>` | Список наград |
| `/vulcan list` | Типы и шансы |
| `/vulcan timer` | Таймер |
| `/vulcan reload` | Перезагрузка |
| `/vulcan help` | Справка |

Типы: `normal`, `legendary`, `mythical`

## Права

- `vulcan.admin` — админ (по умолчанию op)
- `vulcan.info` — `/vulcan info` (по умолчанию true)

## Локальная сборка

```bash
mvn clean package
```

Jar: `target/Vulcan.jar`

## Требования

- Paper 1.16.5
- Java 11+

## Автор

kekih
