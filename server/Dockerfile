FROM node:25-alpine

WORKDIR /backend

# Install dependencies
COPY package*.json ./
ENV DATABASE_URL="postgresql://dummy:dummy@localhost:5432/dummy"

RUN npm install

COPY prisma ./prisma/
COPY prisma.config.js ./prisma.config.js



RUN npx prisma generate

COPY src ./src/

EXPOSE 3000

CMD ["node", "src/app.js"]