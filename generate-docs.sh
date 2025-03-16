#!/bin/bash

echo "Generating Dokka documentation for MAD Gateway..."
./gradlew generateDocs

echo ""
echo "Documentation generated in build/dokka"
echo ""

# Detect OS and open browser accordingly
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "Opening documentation in browser..."
    open build/dokka/index.html
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    echo "Opening documentation in browser..."
    xdg-open build/dokka/index.html 2>/dev/null || sensible-browser build/dokka/index.html 2>/dev/null ||
        echo "Please open build/dokka/index.html in your browser"
else
    echo "Please open build/dokka/index.html in your browser"
fi

echo ""
echo "Done!"
