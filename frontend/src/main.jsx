import React from 'react'
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom'
import { MantineProvider } from '@mantine/core'
import App from './App'
import './index.css'
import '@mantine/core/styles.css';

const container = document.getElementById('root');
const root = createRoot(container);
root.render(
  <React.StrictMode>
    <BrowserRouter>
      <MantineProvider>
        <App/>
      </MantineProvider>
    </BrowserRouter>
  </React.StrictMode>
)
