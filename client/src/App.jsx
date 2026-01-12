import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';

import Matchmaking from './components/ui/Matchmaking';
import Home from './components/ui/Home';
import GamePage from './components/ui/GamePage';

function App() {


  return (
    <>
      <Routes>

        <Route path="/" element={<Home />} />
        <Route path="/matchmaking" element={<Matchmaking />} />
        <Route path="/game:gameId" element={<GamePage />} />
      </Routes>

    </>
  );
}

export default App
