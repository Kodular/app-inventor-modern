import React from 'react'
import { createRoot } from 'react-dom/client';
import './index.css'
import App from './App'
import { BrowserRouter } from 'react-router-dom'
import { MantineProvider } from '@mantine/core'

const container = document.getElementById('root');
const root = createRoot(container);
root.render(
  <React.StrictMode>
    <BrowserRouter>
      <MantineProvider withGlobalStyles withNormalizeCSS>
        <App/>
      </MantineProvider>
    </BrowserRouter>
  </React.StrictMode>
)
