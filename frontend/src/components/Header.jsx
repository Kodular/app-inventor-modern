import { Avatar, Header, Title, Group } from "@mantine/core";

export default function () {
  return (
    <Header height={60} padding="xs">
      <Group>
        <Avatar src={"/src/favicon.svg"} alt="no image here" color="green" />
        <Title order={4}>App Inventor Modern</Title>
      </Group>
    </Header>
  )
}