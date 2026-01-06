

export default function practice(io) {
    io.on('connection', (socket) => {
        socket.on('joinQueue', (userId) => {
            //business logic


            socket.emit('matchFound', { 'message': 'fighting' });
        })
    })
}