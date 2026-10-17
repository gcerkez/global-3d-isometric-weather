.pragma library

/**
 * @file LocationGenerator.js
 * @description Generative Location & Landmark Discovery Engine.
 * Makes any location on Earth dynamically renderable in both Hybrid AI Isometric Mode
 * and Live 3D Isometric Mode by combining:
 *   1. Wikipedia GeoSearch API for real-world local landmarks & historical context.
 *   2. OpenStreetMap Overpass API for live 3D building footprints & heights.
 *   3. Generative AI Isometric Diorama Prompt & Image synthesis.
 *   4. Deterministic coordinate-seeded procedural 3D city & biome generator fallback.
 */

var OVERPASS_ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
];

/**
 * Computes a deterministic 32-bit unsigned integer hash from a string or coordinate pair.
 *
 * @param {string} str - Input string to hash.
 * @returns {number} Unsigned 32-bit integer hash.
 */
function hashString(str) {
    var h = 2166136261;
    var s = String(str || "");
    for (var i = 0; i < s.length; i++) {
        h ^= s.charCodeAt(i);
        h = Math.imul(h, 16777619) >>> 0;
    }
    return h >>> 0;
}

/**
 * Checks whether coordinates correspond to Historic Yorktown, Virginia.
 *
 * @param {number} lat - Latitude in decimal degrees.
 * @param {number} lon - Longitude in decimal degrees.
 * @param {string} [name] - Optional location display name.
 * @returns {boolean} True if the location is Yorktown, VA.
 */
function isYorktownLocation(lat, lon, name) {
    var n = String(name || "").toLowerCase();
    if (n.indexOf("yorktown") !== -1) return true;
    return (Math.abs(lat - 37.2388) < 0.06 && Math.abs(lon - (-76.5097)) < 0.06);
}

/**
 * Returns the default curated landmarks for Historic Yorktown, VA.
 *
 * @returns {Array<{title: string, desc: string, nx: number, ny: number}>}
 */
function getYorktownLandmarks() {
    return [
        {
            title: "George P. Coleman Memorial Bridge",
            desc: "Double-swing steel truss bridge spanning the York River between Yorktown & Gloucester Point.",
            nx: 0.345, ny: 0.365
        },
        {
            title: "18th-Century Tall Ship (York River)",
            desc: "Historic three-masted schooner sailing the 1781 Battle of the Chesapeake & Yorktown waters.",
            nx: 0.205, ny: 0.475
        },
        {
            title: "Yorktown Victory Monument",
            desc: "Ornate column atop the Yorktown bluff commemorating the 1781 Revolutionary War victory.",
            nx: 0.655, ny: 0.235
        },
        {
            title: "1781 Siege Redoubts & Artillery",
            desc: "Preserved Revolutionary War earthen fortifications & bronze siege cannons overlooking the river.",
            nx: 0.735, ny: 0.485
        },
        {
            title: "Historic Main Street & Custom House",
            desc: "18th-century Colonial brick buildings including the Custom House (1721), Nelson House & Swan Tavern.",
            nx: 0.465, ny: 0.635
        }
    ];
}

/**
 * Queries Wikipedia's GeoSearch + Extracts API around `(lat, lon)` to discover real
 * landmarks and historical/cultural descriptions for any location on Earth.
 *
 * @param {number} lat - Center latitude.
 * @param {number} lon - Center longitude.
 * @param {function(Array<{title: string, desc: string, nx: number, ny: number, lat: number, lon: number}>): void} callback
 */
function fetchWikipediaLandmarks(lat, lon, callback) {
    var url = "https://en.wikipedia.org/w/api.php?action=query"
        + "&generator=geosearch"
        + "&ggscoord=" + encodeURIComponent(lat + "|" + lon)
        + "&ggsradius=10000&ggslimit=6"
        + "&prop=extracts|coordinates"
        + "&exintro=1&explaintext=1&exsentences=2"
        + "&format=json&origin=*";

    var xhr = new XMLHttpRequest();
    xhr.open("GET", url);
    xhr.onreadystatechange = function() {
        if (xhr.readyState === XMLHttpRequest.DONE) {
            if (xhr.status === 200) {
                try {
                    var resp = JSON.parse(xhr.responseText);
                    var pages = (resp.query && resp.query.pages) ? resp.query.pages : {};
                    var keys = Object.keys(pages);
                    var items = [];
                    var anchorSlots = [
                        { nx: 0.35, ny: 0.38 },
                        { nx: 0.50, ny: 0.58 },
                        { nx: 0.66, ny: 0.28 },
                        { nx: 0.24, ny: 0.48 },
                        { nx: 0.72, ny: 0.50 },
                        { nx: 0.44, ny: 0.70 }
                    ];
                    for (var i = 0; i < keys.length && items.length < 5; i++) {
                        var p = pages[keys[i]];
                        if (!p || !p.title) continue;
                        var extract = (p.extract || "Notable regional landmark and point of interest.").trim();
                        if (extract.length > 180) {
                            extract = extract.slice(0, 177) + "...";
                        }
                        var slot = anchorSlots[items.length % anchorSlots.length];
                        var pLat = (p.coordinates && p.coordinates[0]) ? p.coordinates[0].lat : lat;
                        var pLon = (p.coordinates && p.coordinates[0]) ? p.coordinates[0].lon : lon;
                        items.push({
                            title: p.title,
                            desc: extract,
                            nx: slot.nx,
                            ny: slot.ny,
                            lat: pLat,
                            lon: pLon
                        });
                    }
                    callback(items);
                    return;
                } catch (e) {
                    console.warn("LocationGenerator: Wikipedia parse error:", e);
                }
            }
            callback([]);
        }
    };
    xhr.send();
}

/**
 * Infers an architectural and environmental archetype for any location on Earth
 * based on its name, coordinates, and discovered Wikipedia landmarks.
 *
 * @param {string} locationName - Human-readable city/town name.
 * @param {number} lat - Latitude.
 * @param {number} lon - Longitude.
 * @param {Array<{title: string, desc: string}>} [landmarks] - Discovered landmarks.
 * @returns {{
 *   archetypeId: string,
 *   subtitle: string,
 *   wallColors: Array<string>,
 *   roofColors: Array<string>,
 *   groundColor: string,
 *   bluffColor: string,
 *   hasWaterfront: boolean,
 *   bridgeStyle: string,
 *   monumentStyle: string,
 *   buildingHeightScale: number,
 *   seed: number
 * }}
 */
function inferRegionalArchetype(locationName, lat, lon, landmarks) {
    if (isYorktownLocation(lat, lon, locationName)) {
        return {
            archetypeId: "colonial_waterfront",
            subtitle: "Historic Colonial Waterfront & 1781 Battlefield",
            wallColors: ["#b8553d", "#bd5840", "#b35039", "#bc5a42"],
            roofColors: ["#5c4d46", "#544741", "#594a42"],
            groundColor: "#8dc055",
            bluffColor: "#94b857",
            hasWaterfront: true,
            bridgeStyle: "coleman_truss",
            monumentStyle: "victory_column",
            buildingHeightScale: 1.0,
            seed: 1781
        };
    }

    var combinedText = (locationName + " " + (landmarks || []).map(function(l) {
        return l.title + " " + l.desc;
    }).join(" ")).toLowerCase();

    var seed = hashString(locationName + ":" + lat.toFixed(2) + ":" + lon.toFixed(2));
    var hasWater = /river|bay|harbor|harbour|beach|island|ocean|sea|port|coast|bridge|lake|sound|waterfront|canal|strait/.test(combinedText)
        || (Math.abs(lon) > 70 && Math.abs(lat) < 48);

    // San Francisco / Bay Area / Modern Coastal Metropolis
    if (/san francisco|golden gate|alcatraz|bay area|seattle|vancouver|sydney|new york|manhattan|chicago|boston|miami/.test(combinedText)) {
        return {
            archetypeId: "metropolitan_bay",
            subtitle: landmarks && landmarks.length > 0
                ? ("Featuring " + landmarks.slice(0, 2).map(function(l) { return l.title; }).join(" & "))
                : "3D Isometric Metropolitan Skyline & Bay",
            wallColors: ["#7fa6c4", "#d9cdb8", "#8d99ae", "#c97b63", "#e0e6ed"],
            roofColors: ["#4a5568", "#2d3748", "#718096"],
            groundColor: "#82b366",
            bluffColor: "#78a85b",
            hasWaterfront: true,
            bridgeStyle: "suspension_golden",
            monumentStyle: "modern_spire",
            buildingHeightScale: 2.35,
            seed: seed
        };
    }

    // Mediterranean / Cycladic Island
    if (/mykonos|santorini|athens|greece|mediterranean|amalfi|dubrovnik|malta|ibiza/.test(combinedText)) {
        return {
            archetypeId: "mediterranean_island",
            subtitle: "Sun-Bleached Cycladic Coastal Diorama",
            wallColors: ["#f8fafc", "#f1f5f9", "#ffffff", "#e2e8f0"],
            roofColors: ["#0284c7", "#0369a1", "#38bdf8"],
            groundColor: "#d6c79c",
            bluffColor: "#a3b18a",
            hasWaterfront: true,
            bridgeStyle: "stone_arch",
            monumentStyle: "windmill_chapel",
            buildingHeightScale: 0.95,
            seed: seed
        };
    }

    // East Asian Metropolis / Historic Temple District
    if (/tokyo|kyoto|osaka|seoul|taipei|shanghai|beijing|hong kong|singapore/.test(combinedText) || (lon > 100 && lon < 146 && lat > 1 && lat < 46)) {
        return {
            archetypeId: "east_asian_city",
            subtitle: landmarks && landmarks.length > 0
                ? ("Featuring " + landmarks.slice(0, 2).map(function(l) { return l.title; }).join(" & "))
                : "3D Isometric Urban & Pagoda District",
            wallColors: ["#cbd5e1", "#94a3b8", "#e2e8f0", "#b45309", "#f8fafc"],
            roofColors: ["#334155", "#991b1b", "#1e293b"],
            groundColor: "#7cb342",
            bluffColor: "#689f38",
            hasWaterfront: true,
            bridgeStyle: "suspension_golden",
            monumentStyle: "pagoda_tower",
            buildingHeightScale: 1.95,
            seed: seed
        };
    }

    // European Historic Old Town
    if (lon >= -12 && lon <= 35 && lat >= 36 && lat <= 65) {
        return {
            archetypeId: "european_historic",
            subtitle: landmarks && landmarks.length > 0
                ? ("Featuring " + landmarks.slice(0, 2).map(function(l) { return l.title; }).join(" & "))
                : "Historic Old Town & Riverfront Diorama",
            wallColors: ["#f3e5ab", "#e6ccb2", "#d5bdaf", "#c97b63", "#ede0d4"],
            roofColors: ["#b55239", "#4a5568", "#9c4221"],
            groundColor: "#84a955",
            bluffColor: "#749c47",
            hasWaterfront: true,
            bridgeStyle: "stone_arch",
            monumentStyle: "cathedral_spire",
            buildingHeightScale: 1.25,
            seed: seed
        };
    }

    // Default Regional Town/City Diorama seeded by coordinates
    return {
        archetypeId: "regional_city",
        subtitle: landmarks && landmarks.length > 0
            ? ("Featuring " + landmarks.slice(0, 2).map(function(l) { return l.title; }).join(" & "))
            : "Generative 3D Isometric Regional Diorama",
        wallColors: ["#c86d51", "#d9c5b2", "#8d99ae", "#b8553d", "#e2d4c4"],
        roofColors: ["#544741", "#4a5568", "#6b5b53"],
        groundColor: "#8ac054",
        bluffColor: "#92b856",
        hasWaterfront: hasWater,
        bridgeStyle: (seed % 2 === 0) ? "coleman_truss" : "suspension_golden",
        monumentStyle: "victory_column",
        buildingHeightScale: 1.15 + ((seed % 7) * 0.14),
        seed: seed
    };
}

/**
 * Queries OpenStreetMap Overpass API for real building footprints and heights around
 * `(centerLat, centerLon)` and normalizes them into the `[-8..+8]` 3D isometric world grid.
 *
 * @param {number} centerLat - Latitude in decimal degrees.
 * @param {number} centerLon - Longitude in decimal degrees.
 * @param {function(Array<{x: number, y: number, dx: number, dy: number, dz: number, brick: string, roof: string, name: string}>): void} callback
 */
function fetchOsmBuildings(centerLat, centerLon, callback) {
    var latRad = centerLat * Math.PI / 180.0;
    var mPerDegLat = 111320.0;
    var mPerDegLon = Math.max(10000.0, 111320.0 * Math.cos(latRad));
    var halfSizeM = 210.0;

    var south = (centerLat - halfSizeM / mPerDegLat).toFixed(5);
    var north = (centerLat + halfSizeM / mPerDegLat).toFixed(5);
    var west  = (centerLon - halfSizeM / mPerDegLon).toFixed(5);
    var east  = (centerLon + halfSizeM / mPerDegLon).toFixed(5);

    var query = '[out:json][timeout:15];(way["building"](' + south + ',' + west + ',' + north + ',' + east + '););out geom 45;';

    function tryEndpoint(idx) {
        if (idx >= OVERPASS_ENDPOINTS.length) {
            callback([]);
            return;
        }
        var xhr = new XMLHttpRequest();
        xhr.open("POST", OVERPASS_ENDPOINTS[idx]);
        xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        xhr.onreadystatechange = function() {
            if (xhr.readyState === XMLHttpRequest.DONE) {
                if (xhr.status === 200) {
                    try {
                        var data = JSON.parse(xhr.responseText);
                        var elements = (data && data.elements) ? data.elements : [];
                        var boxes = [];
                        var palette = ["#b8553d", "#7fa6c4", "#d9c5b2", "#bd5840", "#8d99ae", "#c97b63"];
                        var roofs   = ["#5c4d46", "#4a5568", "#544741", "#334155"];

                        for (var i = 0; i < elements.length && boxes.length < 28; i++) {
                            var el = elements[i];
                            if (el.type !== "way" || !el.geometry || el.geometry.length < 3) continue;
                            var minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
                            for (var j = 0; j < el.geometry.length; j++) {
                                var pt = el.geometry[j];
                                var wx = (pt.lon - centerLon) * mPerDegLon;
                                var wy = (centerLat - pt.lat) * mPerDegLat;
                                if (wx < minX) minX = wx;
                                if (wx > maxX) maxX = wx;
                                if (wy < minY) minY = wy;
                                if (wy > maxY) maxY = wy;
                            }
                            // Map [-210..+210m] into town plaza grid [0.9..5.1 in X, -7.2..+7.2 in Y]
                            var normX = 0.9 + ((minX + halfSizeM) / (2.0 * halfSizeM)) * 4.0;
                            var normY = -7.0 + ((minY + halfSizeM) / (2.0 * halfSizeM)) * 14.0;
                            var spanX = Math.max(0.75, Math.min(1.65, ((maxX - minX) / (2.0 * halfSizeM)) * 6.5));
                            var spanY = Math.max(0.85, Math.min(2.10, ((maxY - minY) / (2.0 * halfSizeM)) * 12.0));

                            var tags = el.tags || {};
                            var levels = parseFloat(tags["building:levels"] || "2.5");
                            if (isNaN(levels) || levels <= 0) levels = 2.5;
                            var heightM = parseFloat(tags.height || (levels * 3.2));
                            if (isNaN(heightM) || heightM <= 0) heightM = 8.0;
                            var dz = Math.max(0.85, Math.min(4.8, heightM / 6.5));

                            var h = hashString(String(el.id));
                            boxes.push({
                                x: normX,
                                y: normY,
                                dx: spanX,
                                dy: spanY,
                                dz: dz,
                                brick: palette[h % palette.length],
                                roof: (dz > 2.4) ? null : roofs[h % roofs.length],
                                name: tags.name || tags.building || "Building"
                            });
                        }
                        callback(boxes);
                        return;
                    } catch (e) {
                        console.warn("LocationGenerator: Overpass parse error:", e);
                    }
                }
                tryEndpoint(idx + 1);
            }
        };
        xhr.send("data=" + encodeURIComponent(query));
    }

    tryEndpoint(0);
}

/**
 * Builds a rich, location-specific 3D isometric diorama prompt for AI image generation
 * (Gemini / Imagen / Flux), incorporating discovered Wikipedia landmarks.
 *
 * @param {string} locationName - City/Town display name.
 * @param {Array<{title: string, desc: string}>} landmarks - Discovered Wikipedia landmarks.
 * @param {string} [archetypeSubtitle] - Regional theme description.
 * @returns {string} Detailed prompt for generating a 3D isometric city diorama.
 */
function buildIsometricDioramaPrompt(locationName, landmarks, archetypeSubtitle) {
    var landmarkNames = (landmarks && landmarks.length > 0)
        ? landmarks.slice(0, 4).map(function(l) { return l.title; }).join(", ")
        : (archetypeSubtitle || "signature local architecture and waterfront");

    return "Cute, detailed 3D isometric miniature city diorama of " + locationName
        + " featuring " + landmarkNames
        + ", sitting on a clean brown earth and deep blue water cross-section slab, "
        + "tilt-shift miniature architectural model style, soft global illumination, "
        + "clean solid light-blue sky background (#a8dbf8), no text, no UI boxes, no watermarks.";
}

/**
 * Builds a zero-key generative AI image URL (via Pollinations Flux model) for on-demand
 * isometric city diorama rendering of any location on Earth.
 *
 * @param {string} locationName - City/Town display name.
 * @param {Array<{title: string, desc: string}>} landmarks - Discovered Wikipedia landmarks.
 * @param {number} [seed] - Deterministic seed.
 * @returns {string} Direct HTTPS image URL for `QML Image.source` or HTML `<img>`.
 */
function buildZeroKeyAiDioramaUrl(locationName, landmarks, seed) {
    var prompt = buildIsometricDioramaPrompt(locationName, landmarks, "");
    var s = seed || hashString(locationName);
    return "https://image.pollinations.ai/prompt/"
        + encodeURIComponent(prompt)
        + "?width=1376&height=768&seed=" + s + "&nologo=true&model=flux";
}

function buildGenerativeDioramaUrl(locationName, landmarks, timeState, weatherCategory) {
    var list = (landmarks || []).map(function(item) {
        return (typeof item === "string") ? { title: item, desc: "" } : item;
    });
    return buildZeroKeyAiDioramaUrl(locationName, list);
}
