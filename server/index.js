const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const UPLOADS_DIR = path.join(__dirname, 'uploads');

if (!fs.existsSync(UPLOADS_DIR)) fs.mkdirSync(UPLOADS_DIR, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, UPLOADS_DIR),
  filename: (req, file, cb) => cb(null, file.originalname),
});
const upload = multer({ storage, fileFilter: (req, file, cb) => {
  cb(null, file.originalname.endsWith('.apk'));
}});

app.use(express.static(path.join(__dirname, 'public')));

app.get('/apps', (req, res) => {
  const files = fs.readdirSync(UPLOADS_DIR).filter(f => f.endsWith('.apk'));
  const apps = files.map(f => {
    const stat = fs.statSync(path.join(UPLOADS_DIR, f));
    return {
      name: f.replace('.apk', ''),
      filename: f,
      size: (stat.size / 1024 / 1024).toFixed(2) + ' MB',
      uploaded: stat.mtime.toISOString(),
      downloadUrl: `/download/${f}`,
    };
  });
  res.json(apps);
});

app.post('/upload', upload.single('apk'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No APK file uploaded' });
  res.json({ success: true, filename: req.file.originalname });
});

app.get('/download/:filename', (req, res) => {
  const filePath = path.join(UPLOADS_DIR, req.params.filename);
  if (!fs.existsSync(filePath)) return res.status(404).json({ error: 'File not found' });
  res.download(filePath);
});

app.delete('/apps/:filename', (req, res) => {
  const filePath = path.join(UPLOADS_DIR, req.params.filename);
  if (!fs.existsSync(filePath)) return res.status(404).json({ error: 'Not found' });
  fs.unlinkSync(filePath);
  res.json({ success: true });
});

app.listen(PORT, () => console.log(`Wear Store running on port ${PORT}`));
