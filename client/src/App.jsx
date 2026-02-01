import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';

import Matchmaking from './components/ui/Matchmaking';
import Home from './components/ui/Home';
import GamePage from './components/ui/GamePage';
import { SocketProvider } from './components/socket/socketcontext';
import LoginPage from './components/ui/LoginPage';
import Navbar from './components/ui/Navbar';
function App() {


  return (
    <>
      <SocketProvider>
        <Navbar />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<Home />} />
          <Route path="/matchmaking" element={<Matchmaking />} />
          <Route path="/game/:gameId" element={<GamePage />} />
        </Routes>


      </SocketProvider>

    </>
  );
}

export default App
