const WS_URL = 'wss://benjamin-painful-lousily.ngrok-free.dev';
class WebSocketService {
    constructor() {
        this.socket = null;
        this.callbacks = {};
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        console.log('WebSocketService constructor called');
        this.connect();
    }

    connect() {
        console.log('Attempting to connect to WebSocket...');
        try {
            this.socket = new WebSocket(WS_URL);

            this.socket.onopen = () => {
                console.log("WebSocket connected to Java Server!");
                this.reconnectAttempts = 0;
            };

            this.socket.onmessage = (event) => {
                console.log("Received from server:", event.data);
                try {
                    const data = JSON.parse(event.data);
                    
                    
                    if (this.callbacks[data.type]) {
                        this.callbacks[data.type](data);
                    }
                } catch (e) {
                    console.error("JSON parse error:", e);
                }
            };

            this.socket.onclose = (event) => {
                console.log("WebSocket closed:", event.code, event.reason);
                
                
                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    console.log(`Retrying connection... (attempt ${this.reconnectAttempts})`);
                    setTimeout(() => this.connect(), 3000);
                }
            };

            this.socket.onerror = (error) => {
                console.error("WebSocket error:", error);
            };
        } catch (error) {
            console.error("Failed to create WebSocket:", error);
        }
    }

    
send(data) {
    console.log("Sending to server:", data);
    
    
    if (!this.socket) {
        console.error("WebSocket not initialized!");
        alert("WebSocket chưa được khởi tạo. Vui lòng tải lại trang!");
        return false;
    }
    
    if (this.socket.readyState === WebSocket.OPEN) {
        const message = JSON.stringify(data);
        console.log("Sending JSON:", message);
        this.socket.send(message);
        return true;
    } else {
        console.warn("WebSocket not connected! State:", this.socket.readyState);
        
       
        const states = {
            0: 'CONNECTING',
            1: 'OPEN',
            2: 'CLOSING',
            3: 'CLOSED'
        };
        console.warn("WebSocket state:", states[this.socket.readyState]);
        
        alert("Chưa kết nối được đến server (State: " + states[this.socket.readyState] + "). Vui lòng tải lại trang!");
        return false;
    }
}

    on(type, callback) {
        console.log(`Registering callback for ${type}`);
        this.callbacks[type] = callback;
    }

    isConnected() {
        return this.socket && this.socket.readyState === WebSocket.OPEN;
    }
    
}


console.log('Creating global wsService...');
window.wsService = new WebSocketService();