import express from 'express';
import { createServer } from 'http';
import { Server } from 'socket.io';
import setupGameSocket from './sockets/gamesocket.js';
import router from './routes/gameRoutes.js';


const app = express();

const server = createServer(app);

const io = new Server(server, {
    cors: {
        origin: "http://localhost:5173",
        methods: ["GET", "POST"]
    }
});

app.set('io', io);
// Setup game socket handlers
setupGameSocket(io);
app.use('/api', router);
const PORT = 3000;

server.listen(PORT, (error) => {
    if (!error)
        console.log("Server is Successfully Running, and App is listening on port " + PORT);
    else {
        console.log("Error occurred, server can't start", error);
    }
});

app.get("/", (req, res) => {
    res.send("Hello to my app");
});

