import { Avatar, Group, Title } from "@mantine/core"
import logo from "@/logo.png"

export default function () {
  return (
    <>
      <Group>
        <Avatar src={logo} alt="logo" color="green" />
        <Title order={4}>App Inventor Modern</Title>
      </Group>
    </>
  )
}
