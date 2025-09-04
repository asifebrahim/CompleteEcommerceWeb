# Product Images Persistence Setup

## Problem
Product images were vanishing after redeployment because they were stored in the application directory (`${user.dir}/productImages`), which gets replaced during deployment.

## Solution
The configuration has been updated to use a persistent external directory that survives deployments.

## Configuration

### Default Path
- **New default**: `/opt/ecommerce/productImages`
- **Old default**: `${user.dir}/productImages` (not persistent)

### Environment Variable
Set `PRODUCT_IMAGES_DIR` to override the default path:
```bash
export PRODUCT_IMAGES_DIR=/path/to/persistent/directory
```

## Deployment Setup

### Local Development
```bash
# Create the directory
sudo mkdir -p /opt/ecommerce/productImages
sudo chown $USER:$USER /opt/ecommerce/productImages
chmod 755 /opt/ecommerce/productImages
```

### Server Deployment
```bash
# Create persistent directory
sudo mkdir -p /opt/ecommerce/productImages
sudo chown app-user:app-group /opt/ecommerce/productImages
chmod 755 /opt/ecommerce/productImages
```

### Docker Deployment
```yaml
# docker-compose.yml
services:
  ecommerce-app:
    image: your-app:latest
    volumes:
      - /opt/ecommerce/productImages:/opt/ecommerce/productImages
    environment:
      - PRODUCT_IMAGES_DIR=/opt/ecommerce/productImages
```

Or with Docker run:
```bash
docker run -v /host/path/productImages:/opt/ecommerce/productImages \
           -e PRODUCT_IMAGES_DIR=/opt/ecommerce/productImages \
           your-app:latest
```

### Cloud Deployment (Render, Heroku, etc.)
For cloud platforms, consider using object storage instead:
- **AWS**: S3 + CloudFront
- **Azure**: Blob Storage + CDN
- **Google Cloud**: Cloud Storage + CDN

## Migration Steps

### 1. Create Persistent Directory
```bash
sudo mkdir -p /opt/ecommerce/productImages
sudo chown [app-user] /opt/ecommerce/productImages
```

### 2. Move Existing Images (if any)
```bash
# Find current images location (usually in app directory)
find . -name "productImages" -type d

# Copy existing images to persistent location
cp -r old-location/productImages/* /opt/ecommerce/productImages/
```

### 3. Set Environment Variable (optional)
```bash
# In your deployment environment
export PRODUCT_IMAGES_DIR=/opt/ecommerce/productImages
```

### 4. Restart Application
The app will:
- Create the directory if it doesn't exist
- Validate write permissions
- Log the resolved path
- Serve images from the persistent location

## Verification

### Check Logs
Look for these log messages on startup:
```
INFO  - Product images directory configured: /opt/ecommerce/productImages
INFO  - Mapping /productImages/** to file:/opt/ecommerce/productImages/
```

### Test Upload
1. Upload a product image via admin interface
2. Verify file appears in `/opt/ecommerce/productImages/`
3. Access image via URL: `http://your-app/productImages/filename.jpg`
4. Redeploy application
5. Verify image still accessible

## Security Notes

- Ensure the directory has proper permissions (755 recommended)
- The application user needs read/write access
- Consider setting up log rotation for large deployments
- For production, use object storage for better scalability

## Troubleshooting

### Permission Errors
```bash
# Fix ownership
sudo chown -R app-user:app-group /opt/ecommerce/productImages

# Fix permissions
chmod -R 755 /opt/ecommerce/productImages
```

### Directory Not Created
Check application logs for:
- `Failed to create product images directory`
- `Product images directory is not writable`

### Images Not Loading
1. Check if files exist in the directory
2. Verify URL pattern matches `/productImages/**`
3. Check Spring Security permits the path
4. Test direct file access permissions
