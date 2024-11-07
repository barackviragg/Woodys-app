@Composable
fun MainMenuScreen(onLogout: () -> Unit) {
    // UI Layout for the main menu
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to Woody's Burger", style = MaterialTheme.typography.h5)

        // Menu items can be added here

        // Logout button
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6f4216)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Logout", color = Color.White)
        }
    }
}
