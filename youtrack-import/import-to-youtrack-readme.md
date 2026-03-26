Шаг 1: Настройка переменных окружения
# Для локального YouTrack
export YOUTRACK_URL="http://localhost:8080"
export YOUTRACK_PROJECT="NOUS"
export YOUTRACK_TOKEN="perm:xc7RMoyXkFLG9yhiBEnu"

# ИЛИ для YouTrack InCloud
export YOUTRACK_URL="https://your-company.myjetbrains.com/youtrack"
export YOUTRACK_TOKEN="perm:YOUR_TOKEN_HERE"

# Опционально: DRY RUN режим (без реального создания)
export YOUTRACK_DRY_RUN=true

# Опционально: подробный логгинг
export YOUTRACK_VERBOSE=true

Шаг 2: Получение токена

    Открой YouTrack
    Settings → Access Tokens → Create Token
    Скопируй токен
    Установи права: Read, Write, Create Issues, Create Components, Create Sprints

Шаг 3: Запуск импорта

# Вариант 1: Через Kotlin скрипт
cd /path/to/Nous-Platform
kotlin scripts/import-to-youtrack.kt

# Вариант 2: Через bash скрипт
chmod +x scripts/run-youtrack-import.sh
./scripts/run-youtrack-import.sh

# Вариант 3: DRY RUN (тестовый режим)
export YOUTRACK_DRY_RUN=true
kotlin scripts/import-to-youtrack.kt

Шаг 4: Проверка результата
http://localhost:8080/issues?q=project:NOUS