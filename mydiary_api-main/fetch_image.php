<?php
require_once 'config.php';

// Check if we should return raw image or JSON
$format = isset($_GET['format']) ? $_GET['format'] : 'raw';

// For debugging
error_log("Image fetch request received with format: $format");

try {
    // Get entryId from query string
    $entryId = isset($_GET['entryId']) ? $_GET['entryId'] : '';
  
    if (empty($entryId)) {
        if ($format === 'json') {
            header('Content-Type: application/json');
            echo json_encode(['error' => 'Missing entryId parameter']);
        } else {
            // Return a transparent 1x1 pixel gif if no entryId
            header('Content-Type: image/gif');
            echo base64_decode('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7');
        }
        error_log("Missing entryId parameter");
        exit;
    }
  
    // Escape the entryId
    $entryId = $conn->real_escape_string($entryId);
  
    // Fetch image data
    $query = "SELECT image_data FROM journal_images WHERE entry_id = '$entryId'";
    $result = $conn->query($query);
    
    if ($result && $result->num_rows > 0) {
        $row = $result->fetch_assoc();
        $imageData = $row['image_data'];
        
        if ($format === 'json') {
            // Return as JSON
            header('Content-Type: application/json');
            echo json_encode(['success' => true, 'imageData' => $imageData]);
            error_log("Image data returned as JSON");
        } else {
            // Return as raw image
            $decodedImage = base64_decode($imageData);
            
            // Detect and set the correct MIME type
            $finfo = new finfo(FILEINFO_MIME_TYPE);
            $mimeType = $finfo->buffer($decodedImage);
            
            header("Content-Type: $mimeType");
            echo $decodedImage;
            error_log("Image data returned as raw image");
        }
    } else {
        if ($format === 'json') {
            header('Content-Type: application/json');
            echo json_encode(['error' => 'Image not found']);
        } else {
            // Return a transparent 1x1 pixel gif if image not found
            header('Content-Type: image/gif');
            echo base64_decode('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7');
        }
        error_log("Image not found for entryId: $entryId");
    }
} catch (Exception $e) {
    if ($format === 'json') {
        header('Content-Type: application/json');
        echo json_encode(['error' => 'Server error: ' . $e->getMessage()]);
    } else {
        // Return a transparent 1x1 pixel gif on error
        header('Content-Type: image/gif');
        echo base64_decode('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7');
    }
    error_log("Error in fetch_image.php: " . $e->getMessage());
}

// Close the connection
$conn->close();
?>