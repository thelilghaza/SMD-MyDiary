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
    if (!$data || !isset($data['entryId'])) {
        http_response_code(400);
        echo json_encode(['error' => 'Missing or invalid JSON data']);
        exit;
    }

    $entryId = $data['entryId'];

    if (empty($entryId)) {
        http_response_code(400);
        echo json_encode(['error' => 'Missing required field: entryId']);
        exit;
    }

    // Escape the data to prevent SQL injection
    $entryId = $conn->real_escape_string($entryId);

    // Delete the image record
    $query = "DELETE FROM journal_images WHERE entry_id = '$entryId'";
    error_log("Deleting image for entry: $entryId");

    if ($conn->query($query) === TRUE) {
        if ($conn->affected_rows > 0) {
            http_response_code(200);
            echo json_encode(['success' => true, 'message' => 'Image deleted successfully']);
        } else {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'No image found for this entry']);
        }
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