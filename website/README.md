# Pocket Sparrow — Official Landing Page & Download Hub

This folder contains the standalone, production-ready landing page for **Pocket Sparrow**. It allows users to download the Windows app, Linux app, Android APK, and visit the GitHub repository.

---

## 🚀 How to Host / Deploy This Landing Page

This website is **100% static** (single `index.html` with Tailwind CSS, Google Fonts, and Lucide Icons). It requires **zero build steps** and can be deployed in seconds to any platform.

### Option 1: GitHub Pages (Free & Automatic)
1. Push this `website/` folder to your GitHub repository.
2. In your GitHub repo, go to **Settings** → **Pages**.
3. Under **Build and deployment**:
   - Source: **Deploy from a branch**
   - Branch: `main`
   - Folder: `/website` (or move files to `/docs` or root of a `gh-pages` branch)
4. Click **Save**. Your site will be live at `https://coder0890.github.io/PocketSparrow/`.

### Option 2: Vercel (1-Click)
1. Go to [vercel.com](https://vercel.com) and click **Add New Project**.
2. Select your `PocketSparrow` repository.
3. Set **Root Directory** to `website`.
4. Click **Deploy**.

### Option 3: Netlify (Drag & Drop or Git)
1. Go to [app.netlify.com](https://app.netlify.com).
2. Either drag-and-drop the `website/` folder, or connect your repository with base directory set to `website`.

### Option 4: Local Preview
To preview the website locally on your machine:
```bash
# Python 3
cd website && python3 -m http.server 8080

# Or Node.js
npx serve website
```
Then open `http://localhost:8080` in your web browser.

---

## 📥 Configuring Download Links

In `website/index.html`, download buttons are configured to point directly to your GitHub Releases:
- **Windows (.exe)**: `https://github.com/CODER0890/PocketSparrow/releases/latest/download/Pocket-Sparrow-Setup.exe`
- **Linux (.AppImage)**: `https://github.com/CODER0890/PocketSparrow/releases/latest/download/Pocket-Sparrow.AppImage`
- **Linux (.deb)**: `https://github.com/CODER0890/PocketSparrow/releases/latest/download/pocket-sparrow_amd64.deb`
- **Android (.apk)**: `https://github.com/CODER0890/PocketSparrow/releases/latest/download/pocket-sparrow-mobile.apk`
- **GitHub Repository**: `https://github.com/CODER0890/PocketSparrow`

Whenever you publish a new GitHub Release with these asset names, the download buttons will automatically fetch the latest binaries.
