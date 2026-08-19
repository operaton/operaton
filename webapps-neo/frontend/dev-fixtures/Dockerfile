FROM node:20-alpine

WORKDIR /app

# Bot scripts (with package.json) at /app/bot, BPMN/DMN one level up
# so deploy.js's resolve(here, '..') still finds processes/ and rules/.
COPY bot/package.json /app/bot/package.json
COPY bot /app/bot
COPY processes /app/processes
COPY rules /app/rules
COPY forms /app/forms

WORKDIR /app/bot

ENV NODE_ENV=production
ENV PORT=3000

EXPOSE 3000

CMD ["node", "server.js"]
