FROM node:25-alpine

WORKDIR /backend

# Install dependencies
COPY package*.json ./
RUN npm install


COPY prisma ./prisma/
RUN npx prisma generate

COPY src ./src/

EXPOSE 3000

CMD ["node", "src/app.js"]