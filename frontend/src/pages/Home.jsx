import { Button } from "@mantine/core";
import { Link } from "react-router-dom";

export default function Home() {
  return (
    <>
      <div>Welcome to App Inventor Modern!</div>
      <Button component={Link} to="/app">Go to app</Button>
    </>
  )
}