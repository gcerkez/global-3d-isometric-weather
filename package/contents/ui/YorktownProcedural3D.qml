/**
 * YorktownProcedural3D.qml
 * ------------------------
 * Interactive 3D isometric procedural renderer with painter's-algorithm depth
 * sorting, camera orbit/pitch/zoom, and generative regional adaptation.
 *
 * When a user selects Yorktown, VA, it renders the historic 1781 waterfront
 * (Coleman Swing Bridge, 18th-century tall ship, Colonial Main Street, Victory
 * Monument, and Siege Redoubts). When a user selects any other city globally,
 * it dynamically adapts its architectural palette, bridge & monument archetypes,
 * and extrudes live OpenStreetMap Overpass 3D building footprints.
 */
import QtQuick

Item {
    id: proc3dRoot

    property string weatherCategory: "clear"
    property string timeState: "day" // day, sunset, night
    property string locationName: "Yorktown, VA"
    property real windSpeedMph: 7.0
    property var regionalArchetype: null
    property var osmBuildings: []

    // Interactive 3D Isometric Camera parameters
    property real cameraYaw: 0.785398   // 45 degrees classic isometric
    property real cameraPitch: 0.54     // ~31 degrees isometric elevation
    property real cameraZoom: 1.0
    property real animTime: 0.0
    property bool bridgeSwinging: false
    property real bridgeSwingAngle: 0.0

    Timer {
        interval: 33
        running: proc3dRoot.visible
        repeat: true
        onTriggered: {
            proc3dRoot.animTime += 0.033
            if (proc3dRoot.bridgeSwinging) {
                proc3dRoot.bridgeSwingAngle = Math.min(0.65, proc3dRoot.bridgeSwingAngle + 0.025)
            } else {
                proc3dRoot.bridgeSwingAngle = Math.max(0.0, proc3dRoot.bridgeSwingAngle - 0.025)
            }
            scene3dCanvas.requestPaint()
        }
    }

    onTimeStateChanged: scene3dCanvas.requestPaint()
    onWeatherCategoryChanged: scene3dCanvas.requestPaint()
    onRegionalArchetypeChanged: scene3dCanvas.requestPaint()
    onOsmBuildingsChanged: scene3dCanvas.requestPaint()

    Canvas {
        id: scene3dCanvas
        anchors.fill: parent
        antialiasing: true

        onPaint: {
            var ctx = getContext("2d")
            ctx.clearRect(0, 0, width, height)
            if (width <= 10 || height <= 10) return

            var cx = width * 0.455
            var cy = height * 0.59
            var baseScale = Math.min(width / 34.0, height / 21.0) * proc3dRoot.cameraZoom
            var yaw = proc3dRoot.cameraYaw
            var pitch = proc3dRoot.cameraPitch
            var cosY = Math.cos(yaw)
            var sinY = Math.sin(yaw)
            var sinP = Math.sin(pitch)
            var cosP = Math.cos(pitch)
            var t = proc3dRoot.animTime
            var isNight = (proc3dRoot.timeState === "night")
            var isSunset = (proc3dRoot.timeState === "sunset")
            var isSnow = (proc3dRoot.weatherCategory === "snow")

            // Project 3D world coordinate (wx, wy, wz) -> 2D screen + depth
            // wx: left(-10) to right(+10), wy: back(-10) to front(+10), wz: height(0..10)
            function project(wx, wy, wz) {
                var rx = wx * cosY - wy * sinY
                var ry = wx * sinY + wy * cosY
                return {
                    x: cx + rx * baseScale,
                    y: cy + (ry * sinP - wz * cosP) * baseScale,
                    depth: ry
                }
            }

            // Shade color helper (mult > 1 brightens, mult < 1 darkens, plus time-of-day tint)
            function shadeHex(hex, mult) {
                var r = parseInt(hex.slice(1, 3), 16)
                var g = parseInt(hex.slice(3, 5), 16)
                var b = parseInt(hex.slice(5, 7), 16)
                if (isSunset) {
                    r = Math.min(255, r * 1.12 + 14)
                    g = Math.min(255, g * 0.94)
                    b = Math.min(255, b * 0.82)
                } else if (isNight) {
                    r = r * 0.42
                    g = g * 0.50
                    b = Math.min(255, b * 0.72 + 12)
                }
                r = Math.max(0, Math.min(255, Math.round(r * mult)))
                g = Math.max(0, Math.min(255, Math.round(g * mult)))
                b = Math.max(0, Math.min(255, Math.round(b * mult)))
                return "rgb(" + r + "," + g + "," + b + ")"
            }

            function drawPoly(pts, fillStyle, strokeStyle, lineWidth) {
                if (pts.length < 3) return
                ctx.beginPath()
                ctx.moveTo(pts[0].x, pts[0].y)
                for (var i = 1; i < pts.length; i++) {
                    ctx.lineTo(pts[i].x, pts[i].y)
                }
                ctx.closePath()
                if (fillStyle) {
                    ctx.fillStyle = fillStyle
                    ctx.fill()
                }
                if (strokeStyle) {
                    ctx.strokeStyle = strokeStyle
                    ctx.lineWidth = lineWidth || 1.0
                    ctx.stroke()
                }
            }

            // Collect 3D boxes & custom drawables into a depth-sorted painter queue
            var drawQueue = []

            function enqueueBox(x0, y0, z0, dx, dy, dz, topHex, sideHex, roofHex, hasWindows, layerTier) {
                var x1 = x0 + dx, y1 = y0 + dy, z1 = z0 + dz
                var center = project(x0 + dx * 0.5, y0 + dy * 0.5, z0 + dz * 0.5)
                var tier = (layerTier !== undefined) ? layerTier : 1
                drawQueue.push({
                    depth: tier * 100.0 + center.depth + (z0 * 0.15),
                    render: function() {
                        var p000 = project(x0, y0, z0), p100 = project(x1, y0, z0)
                        var p110 = project(x1, y1, z0), p010 = project(x0, y1, z0)
                        var p001 = project(x0, y0, z1), p101 = project(x1, y0, z1)
                        var p111 = project(x1, y1, z1), p011 = project(x0, y1, z1)

                        var sCol = sideHex || topHex
                        // Determine which 2 vertical pairs of faces face the camera
                        if (cosY >= 0 && sinY >= 0) {
                            // Front-left (+y) and Front-right (+x) visible
                            drawPoly([p010, p110, p111, p011], shadeHex(sCol, 0.88), shadeHex(sCol, 0.78), 0.7)
                            drawPoly([p100, p110, p111, p101], shadeHex(sCol, 0.68), shadeHex(sCol, 0.60), 0.7)
                        } else if (cosY < 0 && sinY >= 0) {
                            drawPoly([p100, p110, p111, p101], shadeHex(sCol, 0.88), shadeHex(sCol, 0.78), 0.7)
                            drawPoly([p000, p100, p101, p001], shadeHex(sCol, 0.68), shadeHex(sCol, 0.60), 0.7)
                        } else if (cosY < 0 && sinY < 0) {
                            drawPoly([p000, p100, p101, p001], shadeHex(sCol, 0.88), shadeHex(sCol, 0.78), 0.7)
                            drawPoly([p000, p010, p011, p001], shadeHex(sCol, 0.68), shadeHex(sCol, 0.60), 0.7)
                        } else {
                            drawPoly([p000, p010, p011, p001], shadeHex(sCol, 0.88), shadeHex(sCol, 0.78), 0.7)
                            drawPoly([p010, p110, p111, p011], shadeHex(sCol, 0.68), shadeHex(sCol, 0.60), 0.7)
                        }

                        // Windows on colonial buildings
                        if (hasWindows && cosY >= -0.3 && sinY >= -0.3) {
                            var winCol = (isNight || isSunset) ? "#ffd875" : "#dcebf7"
                            var rows = 2
                            var cols = Math.max(2, Math.round(dx * 1.3))
                            for (var wr = 0; wr < rows; wr++) {
                                for (var wc = 0; wc < cols; wc++) {
                                    var wx0 = x0 + (wc + 0.3) * (dx / cols)
                                    var wx1 = wx0 + (dx / cols) * 0.45
                                    var wz0 = z0 + (wr + 0.25) * (dz / (rows + 0.3))
                                    var wz1 = wz0 + (dz / (rows + 0.3)) * 0.48
                                    var wp0 = project(wx0, y1 + 0.02, wz0)
                                    var wp1 = project(wx1, y1 + 0.02, wz0)
                                    var wp2 = project(wx1, y1 + 0.02, wz1)
                                    var wp3 = project(wx0, y1 + 0.02, wz1)
                                    drawPoly([wp0, wp1, wp2, wp3], winCol, "#ffffff", 0.5)
                                }
                            }
                        }

                        // Pitched colonial roof or flat top face
                        if (roofHex) {
                            var ridgeH = dz * 0.48
                            var r0 = project(x0, y0 + dy * 0.5, z1 + ridgeH)
                            var r1 = project(x1, y0 + dy * 0.5, z1 + ridgeH)
                            var actualRoof = isSnow ? "#eef5fc" : roofHex
                            drawPoly([p001, p101, r1, r0], shadeHex(actualRoof, 0.82), shadeHex(actualRoof, 0.70), 0.7)
                            drawPoly([p011, p111, r1, r0], shadeHex(actualRoof, 1.02), shadeHex(actualRoof, 0.85), 0.7)
                            drawPoly([p001, p011, r0], shadeHex(sCol, 0.90), null, 0)
                            drawPoly([p101, p111, r1], shadeHex(sCol, 0.72), null, 0)
                        } else {
                            var topActual = (isSnow && topHex === "#94b857") ? "#e8f1fa" : topHex
                            drawPoly([p001, p101, p111, p011], shadeHex(topActual, 1.05), shadeHex(topActual, 0.92), 0.7)
                        }
                    }
                })
            }

            // 0. Soft drop shadow under entire 3D island slab
            var sh0 = project(-9.2, -9.2, 0), sh1 = project(10.6, -9.2, 0)
            var sh2 = project(10.6, 9.8, 0),  sh3 = project(-9.2, 9.8, 0)
            drawPoly([sh0, sh1, sh2, sh3], "rgba(12, 24, 42, 0.22)", null, 0)

            // 1. Non-overlapping Terrain Base Blocks (Layer Tier 0)
            // 1A. Left-Side York River Earth Bed & Deep Water Block (-9..-0.8 in X, -9..9 in Y)
            enqueueBox(-9.0, -9.0, 0.0, 8.2, 18.0, 0.65, "#9c6340", "#9c6340", null, false, 0)
            enqueueBox(-9.0, -9.0, 0.65, 8.2, 18.0, 1.75, "#2b6ca3", "#1d4e7a", null, false, 0)

            // Animated 3D water wave crests on York River
            drawQueue.push({
                depth: 0 * 100.0 + project(-4.9, 0, 2.5).depth + 5.0,
                render: function() {
                    ctx.strokeStyle = isSunset ? "rgba(255,220,160,0.48)" : "rgba(210,240,255,0.45)"
                    ctx.lineWidth = 1.6
                    for (var wv = 0; wv < 12; wv++) {
                        var wx = -8.0 + (wv % 3) * 2.3 + Math.sin(t * 1.4 + wv) * 0.25
                        var wy = -7.0 + Math.floor(wv / 3) * 4.2
                        var a = project(wx - 0.6, wy, 2.45)
                        var b = project(wx + 0.6, wy, 2.45)
                        ctx.beginPath()
                        ctx.moveTo(a.x, a.y)
                        ctx.lineTo(b.x, b.y)
                        ctx.stroke()
                    }
                }
            })

            // 1B. Sandy Yorktown Beach & Riverwalk Landing Strip (-0.8..0.8 in X)
            enqueueBox(-0.8, -9.0, 0.0, 1.6, 18.0, 2.45, "#efd396", "#b58554", null, false, 0)

            // 1C. Historic Main Street Cobblestone Town Platoon (0.8..5.4 in X)
            enqueueBox(0.8, -9.0, 0.0, 4.6, 18.0, 2.60, "#8dc055", "#9c6340", null, false, 0)
            // Cobblestone road network
            enqueueBox(1.1, -7.5, 2.60, 3.8, 15.0, 0.08, "#b8b0a8", "#9a928c", null, false, 0)

            // 1D. Right-Side Rising Grassy Bluff & 1781 Battlefield Heights (5.4..9.0 in X)
            enqueueBox(5.4, -9.0, 0.0, 3.6, 18.0, 3.9, "#94b857", "#8a5636", null, false, 0)
            enqueueBox(5.8, -8.5, 3.9, 3.0, 16.5, 1.1, "#9ec25e", "#7a4c30", null, false, 0)

            // 2. Above-Ground Landmarks & Structures (Layer Tier 1)
            var arch = proc3dRoot.regionalArchetype || {
                id: "yorktown_1781",
                bridgeStyle: "coleman_truss",
                monumentStyle: "victory_column",
                brickColor: "#b8553d",
                roofColor: "#5c4d46",
                hasTallShip: true,
                hasRedoubts: true
            }

            // 2A. Bridge over River/Bay (Coleman Steel Truss, Golden Gate Suspension, or European Stone Arch)
            var pierCol = (arch.bridgeStyle === "suspension_golden") ? "#c84b31" : "#bfc7cf"
            enqueueBox(-8.2, -1.8, 2.4, 0.7, 1.2, 1.5, pierCol, "#9aa3ad", null, false, 1)
            enqueueBox(-4.5, -2.1, 2.4, 1.6, 1.8, 0.6, "#8c8680", "#635e59", null, false, 1)
            enqueueBox(-4.1, -1.8, 3.0, 0.8, 1.2, (arch.bridgeStyle === "suspension_golden") ? 2.4 : 0.9, pierCol, "#9aa3ad", null, false, 1)
            enqueueBox(-0.4, -1.8, 2.4, 0.7, 1.2, 1.5, pierCol, "#9aa3ad", null, false, 1)

            // Bridge roadway & steel truss / suspension cable structure
            drawQueue.push({
                depth: 1 * 100.0 + project(-4.0, -1.2, 4.3).depth + 0.4,
                render: function() {
                    var b0 = project(-8.8, -1.6, 3.9), b1 = project(1.2, -1.6, 3.9)
                    var b2 = project(1.2, -0.8, 3.9),  b3 = project(-8.8, -0.8, 3.9)
                    drawPoly([b0, b1, b2, b3], shadeHex("#606870", 1.0), "#d8e0e8", 1.2)

                    var trussHex = (arch.bridgeStyle === "suspension_golden")
                        ? "#e05338"
                        : (arch.bridgeStyle === "stone_arch" ? "#d6cab8" : "#dce4ec")
                    ctx.strokeStyle = shadeHex(trussHex, 1.05)
                    ctx.lineWidth = Math.max(1.2, baseScale * 0.08)
                    for (var side = 0; side < 2; side++) {
                        var ty = side === 0 ? -1.6 : -0.8
                        var prevBottom = project(-8.2, ty, 3.95)
                        var prevTop = project(-8.2, ty, 4.55)
                        for (var seg = 1; seg <= 10; seg++) {
                            var sx = -8.2 + seg * 0.9
                            var distCenter = Math.abs(sx - (-3.7))
                            var trussH = (arch.bridgeStyle === "suspension_golden")
                                ? (4.35 + Math.pow((distCenter - 2.2) / 2.5, 2) * 1.1)
                                : (distCenter < 2.2 ? (5.35 - distCenter * 0.32) : 4.55)
                            var curBottom = project(sx, ty, 3.95)
                            var curTop = project(sx, ty, trussH)

                            ctx.beginPath()
                            ctx.moveTo(prevTop.x, prevTop.y)
                            ctx.lineTo(curTop.x, curTop.y)
                            ctx.moveTo(curBottom.x, curBottom.y)
                            ctx.lineTo(curTop.x, curTop.y)
                            ctx.moveTo(prevBottom.x, prevBottom.y)
                            ctx.lineTo(curTop.x, curTop.y)
                            ctx.stroke()

                            prevBottom = curBottom
                            prevTop = curTop
                        }
                    }

                    // Animated miniature cars crossing Bridge
                    var carX = -8.0 + ((t * 1.8) % 9.0)
                    var cp = project(carX, -1.2, 4.02)
                    ctx.fillStyle = isNight ? "#ffee88" : "#e63946"
                    ctx.beginPath()
                    ctx.arc(cp.x, cp.y, Math.max(2.5, baseScale * 0.14), 0, Math.PI * 2)
                    ctx.fill()
                }
            })

            // 2B. Three-Masted Tall Ship / Harbor Vessel sailing on the River
            if (arch.hasTallShip !== false) {
                drawQueue.push({
                    depth: 1 * 100.0 + project(-5.2, 3.2, 3.0).depth + 0.5,
                    render: function() {
                        var shipY = 3.0 + Math.sin(t * 0.8) * 0.35
                        var bobZ = 2.45 + Math.sin(t * 2.1) * 0.08
                        var sx = -5.2

                        // Wooden ship hull
                        var h0 = project(sx - 0.6, shipY - 1.5, bobZ)
                        var h1 = project(sx + 0.6, shipY - 1.5, bobZ)
                        var h2 = project(sx + 0.7, shipY + 1.4, bobZ + 0.45)
                        var h3 = project(sx,       shipY + 2.1, bobZ + 0.55)
                        var h4 = project(sx - 0.7, shipY + 1.4, bobZ + 0.45)
                        drawPoly([h0, h1, h2, h3, h4], shadeHex("#6e4228", 0.95), shadeHex("#3d2212", 1.0), 1.2)

                        // Three tall masts with billowing white square-rigged sails
                        var mastOffsets = [-0.9, 0.1, 1.0]
                        var mastHeights = [2.4, 2.9, 2.2]
                        for (var m = 0; m < 3; m++) {
                            var my = shipY + mastOffsets[m]
                            var mh = mastHeights[m]
                            var mBase = project(sx, my, bobZ + 0.35)
                            var mTop  = project(sx, my, bobZ + 0.35 + mh)

                            ctx.strokeStyle = shadeHex("#52301c", 1.0)
                            ctx.lineWidth = Math.max(1.5, baseScale * 0.09)
                            ctx.beginPath()
                            ctx.moveTo(mBase.x, mBase.y)
                            ctx.lineTo(mTop.x, mTop.y)
                            ctx.stroke()

                            // Lower & upper square sails
                            var sL0 = project(sx - 0.65, my + 0.15, bobZ + 0.65)
                            var sR0 = project(sx + 0.65, my + 0.15, bobZ + 0.65)
                            var sR1 = project(sx + 0.55, my,        bobZ + 0.35 + mh * 0.88)
                            var sL1 = project(sx - 0.55, my,        bobZ + 0.35 + mh * 0.88)
                            drawPoly([sL0, sR0, sR1, sL1], shadeHex("#f7f2e7", 1.04), shadeHex("#d5cbb8", 0.9), 0.8)
                        }
                    }
                })
            }

            // 2C. Buildings: Uses live OpenStreetMap Overpass 3D footprints for non-Yorktown cities, or Historic Yorktown Main Street
            if (arch.id !== "yorktown_1781" && proc3dRoot.osmBuildings && proc3dRoot.osmBuildings.length >= 3) {
                for (var oi = 0; oi < proc3dRoot.osmBuildings.length; oi++) {
                    var ob = proc3dRoot.osmBuildings[oi]
                    enqueueBox(ob.x, ob.y, 2.68, ob.dx, ob.dy, ob.dz, ob.brick || arch.brickColor, ob.brick || arch.brickColor, ob.roof, true, 1)
                }
            } else {
                var defBrick = arch.brickColor || "#b8553d"
                var defRoof  = arch.roofColor  || "#5c4d46"
                var hScale   = (arch.id === "san_francisco" || arch.id === "modern_metropolis") ? 1.55 : 1.0
                var buildings = [
                    { x: 1.0, y: -0.6, dx: 1.4, dy: 2.0, dz: 1.35 * hScale, brick: defBrick, roof: defRoof },
                    { x: 1.0, y: 2.0,  dx: 1.45, dy: 2.1, dz: 1.4 * hScale,  brick: defBrick, roof: defRoof },
                    { x: 1.0, y: 4.6,  dx: 1.5, dy: 2.2, dz: 1.45 * hScale, brick: defBrick, roof: defRoof },
                    { x: 3.1, y: -3.2, dx: 1.5, dy: 2.0, dz: 1.35 * hScale, brick: defBrick, roof: defRoof },
                    { x: 3.1, y: -0.6, dx: 1.5, dy: 2.1, dz: 1.45 * hScale, brick: defBrick, roof: defRoof },
                    { x: 3.1, y: 2.2,  dx: 1.4, dy: 2.0, dz: 1.3 * hScale,  brick: defBrick, roof: defRoof },
                    { x: 2.6, y: 5.2,  dx: 1.6, dy: 2.1, dz: 1.35 * hScale, brick: defBrick, roof: defRoof }
                ]
                for (var bi = 0; bi < buildings.length; bi++) {
                    var b = buildings[bi]
                    enqueueBox(b.x, b.y, 2.68, b.dx, b.dy, b.dz, b.brick, b.brick, b.roof, true, 1)
                }
            }

            // 2D. Bluff Monument (Yorktown Victory Monument, Pagoda Tower, or Modern Spire)
            var monColor = (arch.monumentStyle === "pagoda_tower") ? "#c84630" : "#f5f8fa"
            enqueueBox(6.2, -6.8, 5.0, 2.2, 2.2, 0.35, "#e6eaee", "#cfd6de", null, false, 1)
            enqueueBox(6.55, -6.45, 5.35, 1.5, 1.5, 0.55, "#eef2f6", "#d5dce3", null, false, 1)
            enqueueBox(6.95, -6.05, 5.90, 0.7, 0.7, 4.2, monColor, "#dce3ea", null, false, 1)
            enqueueBox(6.82, -6.18, 10.1, 0.96, 0.96, 0.32, "#ffffff", "#dee5ec", null, false, 1)
            enqueueBox(7.12, -5.88, 10.42, 0.36, 0.36, 0.65, "#f0f4f8", "#cfd8e0", null, false, 1)

            // 2E. Revolutionary War 1781 Siege Redoubts & Bronze Cannons on the Bluff (or Scenic Overlook Park)
            enqueueBox(6.1, -1.2, 5.0, 2.3, 0.45, 0.45, "#87ab4b", "#6e8f38", null, false, 1)
            enqueueBox(8.0, -1.2, 5.0, 0.45, 3.6, 0.45, "#87ab4b", "#6e8f38", null, false, 1)
            enqueueBox(6.1, 2.0,  5.0, 2.3, 0.45, 0.45, "#87ab4b", "#6e8f38", null, false, 1)
            if (arch.hasRedoubts !== false) {
                enqueueBox(6.8, -0.2, 5.0, 0.65, 0.32, 0.30, "#3b3530", "#292420", null, false, 1)
                enqueueBox(6.8, 1.0,  5.0, 0.65, 0.32, 0.30, "#3b3530", "#292420", null, false, 1)
            }

            // 2F. Swaying 3D Isometric Trees along the Yorktown Bluff
            var treeCoords = [
                { x: 5.7, y: -7.5, z: 5.0, r: 0.55 }, { x: 5.7, y: -6.2, z: 5.0, r: 0.62 },
                { x: 5.7, y: -4.8, z: 5.0, r: 0.58 }, { x: 5.7, y: -3.2, z: 5.0, r: 0.64 },
                { x: 7.8, y: -3.6, z: 5.0, r: 0.60 }, { x: 5.8, y: 3.6,  z: 5.0, r: 0.62 },
                { x: 6.8, y: 4.5,  z: 5.0, r: 0.58 }, { x: 7.7, y: 3.8,  z: 5.0, r: 0.54 }
            ]
            for (var ti = 0; ti < treeCoords.length; ti++) {
                (function(tr, idx) {
                    var tp = project(tr.x, tr.y, tr.z + 0.5)
                    drawQueue.push({
                        depth: 1 * 100.0 + tp.depth + 0.3,
                        render: function() {
                            var sway = Math.sin(t * 2.0 + idx) * 0.08
                            var cPos = project(tr.x + sway, tr.y, tr.z + 0.65)
                            var rad = tr.r * baseScale
                            ctx.fillStyle = shadeHex("#4a7c29", 0.85)
                            ctx.beginPath()
                            ctx.arc(cPos.x + rad * 0.12, cPos.y + rad * 0.12, rad, 0, Math.PI * 2)
                            ctx.fill()

                            ctx.fillStyle = shadeHex(isSnow ? "#dce8f2" : "#6ba638", 1.05)
                            ctx.beginPath()
                            ctx.arc(cPos.x, cPos.y, rad * 0.92, 0, Math.PI * 2)
                            ctx.fill()
                        }
                    })
                })(treeCoords[ti], ti)
            }

            // Sort painter queue back-to-front by isometric depth
            drawQueue.sort(function(a, b) { return a.depth - b.depth })
            for (var di = 0; di < drawQueue.length; di++) {
                drawQueue[di].render()
            }
        }
    }

    // Interactive Mouse Orbit / Pitch / Zoom + Double-Click to Reset Isometric Camera
    MouseArea {
        anchors.fill: parent
        acceptedButtons: Qt.LeftButton | Qt.RightButton
        property real lastX: 0
        property real lastY: 0

        onPressed: function(mouse) {
            lastX = mouse.x
            lastY = mouse.y
        }
        onPositionChanged: function(mouse) {
            if (pressed) {
                var dx = mouse.x - lastX
                var dy = mouse.y - lastY
                proc3dRoot.cameraYaw += dx * 0.007
                proc3dRoot.cameraPitch = Math.max(0.28, Math.min(1.05, proc3dRoot.cameraPitch + dy * 0.005))
                lastX = mouse.x
                lastY = mouse.y
                scene3dCanvas.requestPaint()
            }
        }
        onWheel: function(wheel) {
            var delta = wheel.angleDelta.y > 0 ? 1.08 : 0.92
            proc3dRoot.cameraZoom = Math.max(0.65, Math.min(1.65, proc3dRoot.cameraZoom * delta))
            scene3dCanvas.requestPaint()
        }
        onDoubleClicked: {
            proc3dRoot.cameraYaw = 0.785398
            proc3dRoot.cameraPitch = 0.54
            proc3dRoot.cameraZoom = 1.0
            proc3dRoot.bridgeSwinging = !proc3dRoot.bridgeSwinging
            scene3dCanvas.requestPaint()
        }
    }

    // Interactive hint pill in bottom-left when in Procedural 3D mode
    Rectangle {
        anchors.left: parent.left
        anchors.bottom: parent.bottom
        anchors.margins: 12
        height: 24
        width: hintRow.implicitWidth + 16
        radius: 6
        color: "#b0162432"
        border.color: "#4d7ea8"
        border.width: 1

        Row {
            id: hintRow
            anchors.centerIn: parent
            spacing: 6
            Text {
                text: "🧊 Interactive 3D Mode: Drag to Rotate • Scroll to Zoom • Double-Click to Reset"
                color: "#d9ecff"
                font.pixelSize: 10
                font.weight: Font.Medium
            }
        }
    }
}
