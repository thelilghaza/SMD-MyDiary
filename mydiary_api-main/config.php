<?php
// Database configuration
$host = "localhost";
$db_name = "mydiary";
$username = "root";  // Default XAMPP username
$password = "";      // Default XAMPP password (empty)

// Create connection
$conn = new mysqli($host, $username, $password, $db_name);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Set charset to UTF-8
$conn->set_charset("utf8mb4");
?>