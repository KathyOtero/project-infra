# --- Etapa 1: build ---
# Se compila el frontend con Node y luego solo se copia el resultado
# estatico (dist/) a la imagen final de nginx, que es mucho mas liviana que
# cargar Node completo en produccion.
FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Etapa 2: runtime ---
FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO- http://127.0.0.1/ || exit 1
