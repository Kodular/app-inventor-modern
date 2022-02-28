import { Routes, Route } from 'react-router-dom'
import { AuthProvider, RequireAuth } from './hooks/auth'
import Home from './pages/Home'
import Login from './pages/Login'
import Main from './pages/Main'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route
          path="/app"
          element={
            <RequireAuth>
              <Main />
            </RequireAuth>
          }
        />
      </Routes>
    </AuthProvider>
  )
}

export default App
