<script setup>
import {ref, reactive, onMounted, onBeforeUnmount, watch} from 'vue';
import * as THREE from 'three';
import {OrbitControls} from 'three/examples/jsm/controls/OrbitControls.js';

const props = defineProps({
  shapes: {
    type: Array,
    required: true,
  },
});

const containerRef = ref(null);
const legendItems = ref([]);
const visibility = reactive({});
const selectedShape = ref(null);

// Maps shape index to its Three.js objects (mesh + wireframe + label) for toggling
const shapeObjects = {};
// Maps mesh UUID to shape data for click identification
const meshToShapeData = {};

let renderer, scene, camera, controls, animationFrameId;
const raycaster = new THREE.Raycaster();
const mouse = new THREE.Vector2();

const COLORS = [
  0x4285F4, // blue
  0xEA4335, // red
  0x34A853, // green
  0xFBBC05, // yellow
  0x8E24AA, // purple
  0x444444, // Dark gray
  0x00ACC1, // cyan
  0xFF7043, // deep orange
  0x5C6BC0, // indigo
  0x26A69A, // teal
  0xD81B60, // pink
];

function colorToHex(colorInt) {
  return '#' + colorInt.toString(16).padStart(6, '0');
}

function parseEsriJson(esriJsonString) {
  try {
    const parsed = typeof esriJsonString === 'string' ? JSON.parse(esriJsonString) : esriJsonString;
    console.log('[3D Render] Parsed ESRI JSON:', parsed);
    console.log('[3D Render] Keys in parsed object:', Object.keys(parsed));
    const rings = parsed.rings || [];
    console.log('[3D Render] Rings found:', rings.length, 'First ring point count:', rings[0]?.length);
    if (rings.length > 0 && rings[0].length > 0) {
      console.log('[3D Render] Sample coordinate:', rings[0][0]);
    }
    return rings;
  } catch (e) {
    console.error('[3D Render] Failed to parse ESRI JSON:', e, 'Input was:', esriJsonString);
    return [];
  }
}

function computeCentroid(allRings) {
  let sumX = 0, sumY = 0, count = 0;
  for (const rings of allRings) {
    for (const ring of rings) {
      for (const coord of ring) {
        sumX += coord[0];
        sumY += coord[1];
        count++;
      }
    }
  }
  const result = count > 0 ? {x: sumX / count, y: sumY / count} : {x: 0, y: 0};
  console.log('[3D Render] Centroid computed:', result, 'from', count, 'points');
  return result;
}

function createTextSprite(text) {
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');
  const fontSize = 48;
  ctx.font = `${fontSize}px Arial`;
  const textWidth = ctx.measureText(text).width;
  canvas.width = textWidth + 16;
  canvas.height = fontSize + 16;
  // Re-set font after canvas resize clears it
  ctx.font = `${fontSize}px Arial`;
  ctx.fillStyle = '#505050';
  ctx.textBaseline = 'middle';
  ctx.fillText(text, 8, canvas.height / 2);

  const texture = new THREE.CanvasTexture(canvas);
  texture.minFilter = THREE.LinearFilter;
  const spriteMaterial = new THREE.SpriteMaterial({map: texture, depthTest: false});
  const sprite = new THREE.Sprite(spriteMaterial);
  // Scale sprite to a reasonable world size
  const aspect = canvas.width / canvas.height;
  sprite.scale.set(aspect * 1.2, 1.2, 1);
  return sprite;
}

function buildDepthScale(minDepth, maxDepth, depthScale, depthCentroid, xPosition) {
  const scaleGroup = new THREE.Group();

  const scaledMin = (minDepth - depthCentroid) * depthScale;
  const scaledMax = (maxDepth - depthCentroid) * depthScale;

  // Vertical line
  const lineMaterial = new THREE.LineBasicMaterial({color: 0x505050});
  const lineGeometry = new THREE.BufferGeometry().setFromPoints([
    new THREE.Vector3(xPosition, scaledMin, 0),
    new THREE.Vector3(xPosition, scaledMax, 0),
  ]);
  scaleGroup.add(new THREE.Line(lineGeometry, lineMaterial));

  // Choose nice tick intervals based on the real depth range
  const depthRange = maxDepth - minDepth;
  const rawInterval = depthRange / 8;
  const magnitude = Math.pow(10, Math.floor(Math.log10(rawInterval)));
  const nice = [1, 2, 5, 10].find(n => n * magnitude >= rawInterval) * magnitude;

  const firstTick = Math.ceil(minDepth / nice) * nice;
  const tickLength = 0.4;

  for (let depth = firstTick; depth <= maxDepth; depth += nice) {
    const y = (depth - depthCentroid) * depthScale;

    // Tick mark
    const tickGeom = new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(xPosition - tickLength, y, 0),
      new THREE.Vector3(xPosition + tickLength, y, 0),
    ]);
    scaleGroup.add(new THREE.Line(tickGeom, lineMaterial));

    // Label
    const label = createTextSprite(`${depth}m`);
    label.position.set(xPosition - 2, y, 0);
    scaleGroup.add(label);
  }

  // Top/bottom labels if they don't coincide with a tick
  const tolerance = nice * 0.1;
  if (Math.abs(minDepth - firstTick) > tolerance) {
    const label = createTextSprite(`${Math.round(minDepth)}m`);
    label.position.set(xPosition - 2, scaledMin, 0);
    scaleGroup.add(label);
  }
  const lastTick = Math.floor(maxDepth / nice) * nice;
  if (Math.abs(maxDepth - lastTick) > tolerance) {
    const label = createTextSprite(`${Math.round(maxDepth)}m`);
    label.position.set(xPosition - 2, scaledMax, 0);
    scaleGroup.add(label);
  }

  return scaleGroup;
}

function toggleShape(index) {
  visibility[index] = !visibility[index];
  const objects = shapeObjects[index];
  if (objects) {
    objects.forEach(obj => {
      obj.visible = visibility[index];
    });
  }
}

function buildScene() {
  console.log('[3D Render] buildScene called');
  console.log('[3D Render] containerRef:', containerRef.value ? 'exists' : 'null');
  console.log('[3D Render] props.shapes:', props.shapes);
  console.log('[3D Render] props.shapes type:', typeof props.shapes);
  console.log('[3D Render] props.shapes length:', props.shapes?.length);

  if (!containerRef.value || !props.shapes || props.shapes.length === 0) {
    console.warn('[3D Render] Early return from buildScene - container:', !!containerRef.value,
      'shapes:', !!props.shapes, 'length:', props.shapes?.length);
    return;
  }

  // Clear existing scene objects but keep the scene
  while (scene.children.length > 0) {
    scene.remove(scene.children[0]);
  }

  // Reset legend and object tracking
  const newLegendItems = [];
  Object.keys(shapeObjects).forEach(k => delete shapeObjects[k]);
  Object.keys(meshToShapeData).forEach(k => delete meshToShapeData[k]);
  selectedShape.value = null;

  // Parse all polygons first to compute centroid for normalization
  const parsedShapes = props.shapes.map((shape, i) => {
    console.log(`[3D Render] Shape ${i}:`, shape);
    console.log(`[3D Render] Shape ${i} esriJsonPolygon type:`, typeof shape.esriJsonPolygon);
    console.log(`[3D Render] Shape ${i} depthStart:`, shape.depthStart, 'depthEnd:', shape.depthEnd);
    return {
      rings: parseEsriJson(shape.esriJsonPolygon),
      depthStart: shape.depthStart,
      depthEnd: shape.depthEnd,
      name: shape.name || `Shape ${i + 1}`,
    };
  });

  const allRings = parsedShapes.map(s => s.rings);
  const centroid = computeCentroid(allRings);

  // Determine scale - find the bounding box extent to normalize coordinates
  let minX = Infinity, maxX = -Infinity, minZ = Infinity, maxZ = -Infinity;
  let minY = Infinity, maxY = -Infinity;
  for (const shape of parsedShapes) {
    for (const ring of shape.rings) {
      for (const coord of ring) {
        const x = coord[0] - centroid.x;
        const z = coord[1] - centroid.y;
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minZ = Math.min(minZ, z);
        maxZ = Math.max(maxZ, z);
      }
    }
    minY = Math.min(minY, shape.depthStart, shape.depthEnd);
    maxY = Math.max(maxY, shape.depthStart, shape.depthEnd);
  }

  console.log('[3D Render] Spatial bounds - X:', minX, 'to', maxX, 'Z:', minZ, 'to', maxZ);
  console.log('[3D Render] Depth bounds - Y:', minY, 'to', maxY);

  const spatialExtent = Math.max(maxX - minX, maxZ - minZ, 1);
  const depthExtent = Math.max(maxY - minY, 1);
  const targetSize = 20;
  const spatialScale = targetSize / spatialExtent;
  // Scale depth independently, halved so vertical doesn't dominate the view
  const depthScale = (targetSize / depthExtent) * 0.5;
  const depthCentroid = (minY + maxY) / 2;

  console.log('[3D Render] spatialExtent:', spatialExtent, 'spatialScale:', spatialScale, 'depthCentroid:', depthCentroid);

  // Create meshes for each shape
  parsedShapes.forEach((shapeData, index) => {
    const rings = shapeData.rings;
    if (rings.length === 0) {
      console.warn(`[3D Render] Shape ${index} has no rings, skipping`);
      return;
    }

    const outerRing = rings[0];
    if (outerRing.length < 3) {
      console.warn(`[3D Render] Shape ${index} outer ring has fewer than 3 points:`, outerRing.length);
      return;
    }

    const threeShape = new THREE.Shape();

    const firstX = (outerRing[0][0] - centroid.x) * spatialScale;
    const firstZ = (outerRing[0][1] - centroid.y) * spatialScale;
    threeShape.moveTo(firstX, firstZ);

    for (let i = 1; i < outerRing.length; i++) {
      const x = (outerRing[i][0] - centroid.x) * spatialScale;
      const z = (outerRing[i][1] - centroid.y) * spatialScale;
      threeShape.lineTo(x, z);
    }
    threeShape.closePath();

    // Add holes from inner rings
    for (let r = 1; r < rings.length; r++) {
      const holePath = new THREE.Path();
      const holeRing = rings[r];
      const hx = (holeRing[0][0] - centroid.x) * spatialScale;
      const hz = (holeRing[0][1] - centroid.y) * spatialScale;
      holePath.moveTo(hx, hz);
      for (let i = 1; i < holeRing.length; i++) {
        const x = (holeRing[i][0] - centroid.x) * spatialScale;
        const z = (holeRing[i][1] - centroid.y) * spatialScale;
        holePath.lineTo(x, z);
      }
      threeShape.holes.push(holePath);
    }

    const depth = Math.abs(shapeData.depthEnd - shapeData.depthStart) * depthScale;
    console.log(`[3D Render] Shape ${index} - extrude depth:`, depth);

    if (depth === 0) {
      console.warn(`[3D Render] Shape ${index} has zero depth (start=${shapeData.depthStart}, end=${shapeData.depthEnd}), skipping`);
      return;
    }

    const geometry = new THREE.ExtrudeGeometry(threeShape, {
      depth: depth,
      bevelEnabled: false,
    });

    // ExtrudeGeometry extrudes along Z axis, rotate so it goes along Y axis
    geometry.rotateX(-Math.PI / 2);

    const color = COLORS[index % COLORS.length];
    const material = new THREE.MeshPhongMaterial({
      color: color,
      transparent: true,
      opacity: 0.5,
      side: THREE.DoubleSide,
      depthWrite: false,
    });



    const mesh = new THREE.Mesh(geometry, material);

    if (shapeData.name === "16/26a") {
      mesh.material.opacity = 0;
      const geo = new THREE.EdgesGeometry( mesh.geometry ); // or WireframeGeometry
      const mat = new THREE.LineBasicMaterial( { color: 0xffffff } );
      const wireframe = new THREE.LineSegments( geo, mat );
      mesh.add(wireframe);
    }

    const yPosition = (Math.min(shapeData.depthStart, shapeData.depthEnd) - depthCentroid) * depthScale;
    mesh.position.y = yPosition;

    console.log(`[3D Render] Shape ${index} - color: 0x${color.toString(16)}, yPosition:`, yPosition);
    console.log(`[3D Render] Shape ${index} - geometry vertices:`, geometry.attributes.position?.count);

    scene.add(mesh);

    // Add wireframe edges for clarity
    const edgeGeometry = new THREE.EdgesGeometry(geometry);
    const edgeMaterial = new THREE.LineBasicMaterial({
      color: new THREE.Color(color).multiplyScalar(0.6),
      linewidth: 1,
    });
    const wireframe = new THREE.LineSegments(edgeGeometry, edgeMaterial);
    wireframe.position.y = yPosition;
    scene.add(wireframe);

    // Track objects for visibility toggling and click identification
    shapeObjects[index] = [mesh, wireframe];
    meshToShapeData[mesh.uuid] = {
      name: shapeData.name,
      depthStart: shapeData.depthStart,
      depthEnd: shapeData.depthEnd,
      color: colorToHex(color),
    };

    // Initialise visibility if not already set
    if (visibility[index] === undefined) {
      visibility[index] = true;
    }

    newLegendItems.push({
      index: index,
      name: shapeData.name,
      color: colorToHex(color),
    });
  });

  legendItems.value = newLegendItems;

  // Lighting
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
  directionalLight.position.set(10, 20, 10);
  scene.add(directionalLight);

  const directionalLight2 = new THREE.DirectionalLight(0xffffff, 0.3);
  directionalLight2.position.set(-10, -10, -10);
  scene.add(directionalLight2);

  // Depth scale bar positioned at the edge of the scene
  // const scaleXPosition = (maxX * spatialScale) + 3;
  // const depthScaleBar = buildDepthScale(minY, maxY, depthScale, depthCentroid, scaleXPosition);
  // scene.add(depthScaleBar);

  // Position camera - center on the midpoint of the shapes' bounding box
  const centerX = (minX + maxX) / 2 * spatialScale;
  const centerY = 0; // depth is already centered around 0
  const centerZ = (minZ + maxZ) / 2 * spatialScale;
  camera.position.set(centerX + targetSize * 0.5, centerY + targetSize * 0.5, centerZ + targetSize * 0.5);
  camera.lookAt(centerX, centerY, centerZ);
  controls.target.set(centerX, centerY, centerZ);
  controls.update();

  console.log('[3D Render] Scene built. Total children:', scene.children.length);
  console.log('[3D Render] Camera position:', camera.position);
}

let pointerDownPos = null;

function onPointerDown(event) {
  pointerDownPos = {x: event.clientX, y: event.clientY};
}

function onCanvasClick(event) {
  if (!containerRef.value || !camera || !scene) return;

  // Ignore if the pointer moved (i.e. a drag/rotate), threshold of 5px
  if (pointerDownPos) {
    const dx = event.clientX - pointerDownPos.x;
    const dy = event.clientY - pointerDownPos.y;
    if (Math.sqrt(dx * dx + dy * dy) > 5) return;
  }

  const rect = renderer.domElement.getBoundingClientRect();
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

  raycaster.setFromCamera(mouse, camera);
  const meshes = scene.children.filter(child => child.isMesh);
  const intersects = raycaster.intersectObjects(meshes);

  if (intersects.length > 0) {
    const hit = intersects[0].object;
    const data = meshToShapeData[hit.uuid];
    if (data) {
      selectedShape.value = data;
    }
  } else {
    selectedShape.value = null;
  }
}

function initThreeJs() {
  const container = containerRef.value;
  console.log('[3D Render] initThreeJs called, container:', container ? `${container.clientWidth}x${container.clientHeight}` : 'null');
  if (!container) return;

  // Renderer
  renderer = new THREE.WebGLRenderer({antialias: true});
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(container.clientWidth, container.clientHeight);
  container.appendChild(renderer.domElement);
  renderer.domElement.addEventListener('pointerdown', onPointerDown);
  renderer.domElement.addEventListener('click', onCanvasClick);

  // Scene
  scene = new THREE.Scene();
  scene.background = new THREE.Color(0xf0f0f0);

  // Camera
  camera = new THREE.PerspectiveCamera(
    50,
    container.clientWidth / container.clientHeight,
    0.1,
    1000,
  );
  camera.position.set(10, 12, 10);
  camera.lookAt(0, 0, 0);

  // Controls
  controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.dampingFactor = 0.1;

  // Build the shapes
  buildScene();

  // Handle resize
  window.addEventListener('resize', onResize);

  // Start render loop
  animate();
  console.log('[3D Render] Initialization complete, render loop started');
}

function animate() {
  animationFrameId = requestAnimationFrame(animate);
  controls.update();
  renderer.render(scene, camera);
}

function onResize() {
  if (!containerRef.value || !camera || !renderer) return;
  const container = containerRef.value;
  camera.aspect = container.clientWidth / container.clientHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(container.clientWidth, container.clientHeight);
}

onMounted(() => {
  console.log('[3D Render] Component mounted. Props:', JSON.stringify(props.shapes).substring(0, 500));
  initThreeJs();
});

onBeforeUnmount(() => {
  if (animationFrameId) cancelAnimationFrame(animationFrameId);
  window.removeEventListener('resize', onResize);
  if (renderer) {
    renderer.domElement.removeEventListener('pointerdown', onPointerDown);
    renderer.domElement.removeEventListener('click', onCanvasClick);
    renderer.dispose();
    if (containerRef.value && renderer.domElement.parentNode === containerRef.value) {
      containerRef.value.removeChild(renderer.domElement);
    }
  }
  if (controls) controls.dispose();
});

watch(() => props.shapes, () => {
  console.log('[3D Render] shapes prop changed, rebuilding scene');
  buildScene();
}, {deep: true});
</script>

<template>
  <div class="three-dimensional-render-page">
    <div class="govuk-body govuk-!-margin-bottom-2">
      <strong>3D Shape Viewer</strong> — Click and drag to rotate. Scroll to zoom. Right-click drag to pan. Click a shape for details.
    </div>
    <div class="three-d-layout">
      <div class="three-d-container-wrapper">
        <div ref="containerRef" class="three-d-container"></div>
        <div v-if="selectedShape" class="shape-info-panel">
          <button class="shape-info-close" @click="selectedShape = null">&times;</button>
          <div class="govuk-body-s govuk-!-font-weight-bold govuk-!-margin-bottom-1">{{ selectedShape.name }}</div>
          <div class="govuk-body-s govuk-!-margin-bottom-1">
            <span class="shape-info-swatch" :style="{ backgroundColor: selectedShape.color }"></span>
            Depth: {{ selectedShape.depthStart }}m to {{ selectedShape.depthEnd }}m
          </div>
          <div class="govuk-body-s govuk-!-margin-bottom-0">
            Thickness: {{ Math.abs(selectedShape.depthEnd - selectedShape.depthStart) }}m
          </div>
        </div>
      </div>
      <div class="three-d-legend" v-if="legendItems.length > 0">
        <div class="govuk-body-s govuk-!-font-weight-bold govuk-!-margin-bottom-2">Key</div>
        <ul class="legend-list">
          <li
            v-for="item in legendItems"
            :key="item.index"
            class="legend-item"
            :class="{ 'legend-item--hidden': !visibility[item.index] }"
            @click="toggleShape(item.index)"
          >
            <span class="legend-swatch" :style="{ backgroundColor: item.color }"></span>
            <span class="legend-label">{{ item.name }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.three-dimensional-render-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.three-d-layout {
  display: flex;
  flex: 1;
  gap: 16px;
}

.three-d-container-wrapper {
  flex: 1;
  position: relative;
}

.three-d-container {
  width: 100%;
  min-height: 600px;
  border: 1px solid #b1b4b6;
  border-radius: 4px;
  overflow: hidden;
}

.shape-info-panel {
  position: absolute;
  top: 12px;
  left: 12px;
  background: #ffffff;
  border: 1px solid #b1b4b6;
  border-radius: 4px;
  padding: 12px 32px 12px 12px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  z-index: 10;
  min-width: 180px;
}

.shape-info-close {
  position: absolute;
  top: 4px;
  right: 8px;
  background: none;
  border: none;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  color: #505050;
  padding: 2px 4px;
}

.shape-info-close:hover {
  color: #0b0c0c;
}

.shape-info-swatch {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  vertical-align: middle;
  margin-right: 4px;
}

.three-d-legend {
  width: 220px;
  flex-shrink: 0;
  padding: 12px;
  border: 1px solid #b1b4b6;
  border-radius: 4px;
  background: #ffffff;
  align-self: flex-start;
}

.legend-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s;
}

.legend-item:hover {
  background-color: #f3f2f1;
}

.legend-item--hidden {
  opacity: 0.4;
}

.legend-item--hidden .legend-swatch {
  background-color: #b1b4b6 !important;
}

.legend-swatch {
  width: 16px;
  height: 16px;
  border-radius: 3px;
  flex-shrink: 0;
  border: 1px solid rgba(0, 0, 0, 0.15);
}

.legend-label {
  font-size: 14px;
  line-height: 1.3;
}
</style>
