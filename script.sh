find . -path "./.gradle" -prune -o -path "./.git" -prune -o \
 -path "./.idea" -prune -o -path "./app/build" -prune -o -path "./.gradle" -prune -o \
 -path "./target" -prune -o -print > project-structure.txt
