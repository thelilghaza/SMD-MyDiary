<?php
require_once 'config.php';

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// For debugging
error_log("POST data received: " . file_get_contents('php://input'));

try {
    // Get POST data
    $data = json_decode(file_get_contents('php://input'), true);
    if (!$data || !isset($data['entryId']) || !isset($data['imageBase64'])) {
        http_response_code(400);
        echo json_encode(['error' => 'Missing or invalid JSON data']);
        exit;
    }

    $entryId = $data['entryId'];
    $imageBase64 = $data['imageBase64'];

    if (empty($entryId) || empty($imageBase64)) {
        http_response_code(400);
        echo json_encode(['error' => 'Missing required fields']);
        exit;
    }

    // Escape the data to prevent SQL injection
    $entryId = $conn->real_escape_string($entryId);
    $imageBase64 = $conn->real_escape_string($imageBase64);

    // Check if entryId already exists
    $query = "SELECT COUNT(*) FROM journal_images WHERE entry_id = '$entryId'";
    $result = $conn->query($query);
    $count = $result->fetch_row()[0];

    if ($count > 0) {
        // Update existing entry
        $query = "UPDATE journal_images SET image_data = '$imageBase64' WHERE entry_id = '$entryId'";
        error_log("Updating existing entry: $entryId");
    } else {
        // Insert new entry
        $query = "INSERT INTO journal_images (entry_id, image_data) VALUES ('$entryId', '$imageBase64')";
        error_log("Inserting new entry: $entryId");
    }

    if ($conn->query($query) === TRUE) {
        // Return image URL (updated for emulator testing)
        $imageUrl = "http://192.168.155.103/mydiary_api/fetch_image.php?entryId=" . urlencode($entryId);
        http_response_code(200);
        echo json_encode(['imageUrl' => $imageUrl]);
    } else {
        http_response_code(500);
        echo json_encode(['error' => 'Database error: ' . $conn->error]);
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Server error: ' . $e->getMessage()]);
}

// Close the connection
$conn->close();
?>