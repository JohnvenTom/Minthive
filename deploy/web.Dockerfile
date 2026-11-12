# 前端构建镜像：分别构建用户端与管理端，由同一 Nginx 按端口提供
FROM node:20-alpine AS user-build
WORKDIR /src/frontend-user
COPY frontend-user/package.json frontend-user/package-lock.json ./
RUN npm ci
COPY frontend-user/ ./
RUN npm run build

FROM node:20-alpine AS admin-build
WORKDIR /src/frontend-admin
COPY frontend-admin/package.json frontend-admin/package-lock.json ./
RUN npm ci
COPY frontend-admin/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx/user.conf /etc/nginx/conf.d/user.conf
COPY deploy/nginx/admin.conf /etc/nginx/conf.d/admin.conf
COPY --from=user-build /src/frontend-user/dist /usr/share/nginx/html/user
COPY --from=admin-build /src/frontend-admin/dist /usr/share/nginx/html/admin
EXPOSE 80 81
