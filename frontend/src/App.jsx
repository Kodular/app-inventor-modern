import { Routes, Route } from 'react-router-dom'
import { AuthProvider, RequireAuth } from './hooks/auth'
import Home from './pages/Home'
import Login from './pages/Login'
import Main from './pages/Main'
import Project from './pages/Project'

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
        <Route
          path="/project/:id"
          element={
            <RequireAuth>
              <Project />
            </RequireAuth>
          }
        />
      </Routes>
    </AuthProvider>
  )
}

export default App
