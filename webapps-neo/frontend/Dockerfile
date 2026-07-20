FROM node:18-alpine as builder

WORKDIR /app
COPY package*.json .
RUN npm ci
COPY . .

RUN npm run build

CMD "RUN echo $(ls -1 /app)"


# Step 2: Use a lightweight Nginx container to serve the files
FROM nginx:alpine

# Copy Nginx config if you have custom routing
COPY ./nginx.conf /etc/nginx/nginx.conf

# Copy the built app from the builder stage
COPY --from=builder /app/dist /var/www/html/
#COPY --from=builder /app/dist /user/share/nginx/html

# Copy the runtime injection script into the container
COPY env.sh /docker-entrypoint.d/env.sh
RUN dos2unix /docker-entrypoint.d/env.sh
RUN chmod +x /docker-entrypoint.d/env.sh

# Let Docker run your script before starting Nginx
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]