<script setup>
import {ref, reactive, onMounted, onBeforeUnmount, watch} from 'vue';
import * as THREE from 'three';

const props = defineProps({
  shapes: {
    type: Array,
    required: true,
  },
});

const containerRef = ref(null);
const legendItems = ref([]);
const visibility = reactive({});

const shapeObjects = {};

let renderer, scene, camera;

const COLORS = [
  0x4285F4, // blue
  0xEA4335, // red
  0x34A853, // green
  0xFBBC05, // yellow
  0x8E24AA, // purple
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
    return parsed.rings || [];
  } catch (e) {
    console.error('[Static 3D] Failed to parse ESRI JSON:', e);
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
  return count > 0 ? {x: sumX / count, y: sumY / count} : {x: 0, y: 0};
}

function toggleShape(index) {
  visibility[index] = !visibility[index];
  const objects = shapeObjects[index];
  if (objects) {
    objects.forEach(obj => {
      obj.visible = visibility[index];
    });
  }
  renderOnce();
}

function renderOnce() {
  if (renderer && scene && camera) {
    renderer.render(scene, camera);
  }
}

function buildScene() {
  if (!containerRef.value || !props.shapes || props.shapes.length === 0) return;

  while (scene.children.length > 0) {
    scene.remove(scene.children[0]);
  }

  const newLegendItems = [];
  Object.keys(shapeObjects).forEach(k => delete shapeObjects[k]);

  const parsedShapes = props.shapes.map((shape, i) => ({
    rings: parseEsriJson(shape.esriJsonPolygon),
    depthStart: shape.depthStart,
    depthEnd: shape.depthEnd,
    name: shape.name || `Shape ${i + 1}`,
  }));

  const allRings = parsedShapes.map(s => s.rings);
  const centroid = computeCentroid(allRings);

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

  const spatialExtent = Math.max(maxX - minX, maxZ - minZ, 1);
  const depthExtent = Math.max(maxY - minY, 1);
  const targetSize = 20;
  const spatialScale = targetSize / spatialExtent;
  const depthScale = (targetSize / depthExtent) * 0.5;
  const depthCentroid = (minY + maxY) / 2;

  parsedShapes.forEach((shapeData, index) => {
    const rings = shapeData.rings;
    if (rings.length === 0) return;

    const outerRing = rings[0];
    if (outerRing.length < 3) return;

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
    if (depth === 0) return;

    const geometry = new THREE.ExtrudeGeometry(threeShape, {
      depth: depth,
      bevelEnabled: false,
    });

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
    const yPosition = (Math.min(shapeData.depthStart, shapeData.depthEnd) - depthCentroid) * depthScale;
    mesh.position.y = yPosition;
    scene.add(mesh);

    const edgeGeometry = new THREE.EdgesGeometry(geometry);
    const edgeMaterial = new THREE.LineBasicMaterial({
      color: new THREE.Color(color).multiplyScalar(0.6),
      linewidth: 1,
    });
    const wireframe = new THREE.LineSegments(edgeGeometry, edgeMaterial);
    wireframe.position.y = yPosition;
    scene.add(wireframe);

    shapeObjects[index] = [mesh, wireframe];

    if (visibility[index] === undefined) {
      visibility[index] = true;
    }

    newLegendItems.push({
      index: index,
      name: shapeData.name,
      color: colorToHex(color),
      depthStart: Math.min(shapeData.depthStart, shapeData.depthEnd),
      depthEnd: Math.max(shapeData.depthStart, shapeData.depthEnd),
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

  // Position orthographic camera for isometric view
  const centerX = (minX + maxX) / 2 * spatialScale;
  const centerY = 0;
  const centerZ = (minZ + maxZ) / 2 * spatialScale;

  const isoDistance = targetSize * 1.2;
  camera.position.set(
    centerX + isoDistance,
    centerY + isoDistance,
    centerZ + isoDistance,
  );
  camera.lookAt(centerX, centerY, centerZ);
  camera.updateProjectionMatrix();

  // Compute the projected bounding box of all scene objects to fit the camera tightly
  const aspect = containerRef.value.clientWidth / containerRef.value.clientHeight;
  const viewMatrix = camera.matrixWorldInverse;
  const box = new THREE.Box3();

  scene.updateMatrixWorld(true);
  scene.traverse(obj => {
    if (obj.isMesh || obj.isLineSegments || obj.isLine) {
      const geometry = obj.geometry;
      if (!geometry.boundingBox) geometry.computeBoundingBox();
      const objBox = geometry.boundingBox.clone();
      objBox.applyMatrix4(obj.matrixWorld);
      box.union(objBox);
    }
  });

  // Project the 8 corners of the world bounding box into camera (view) space
  const corners = [
    new THREE.Vector3(box.min.x, box.min.y, box.min.z),
    new THREE.Vector3(box.min.x, box.min.y, box.max.z),
    new THREE.Vector3(box.min.x, box.max.y, box.min.z),
    new THREE.Vector3(box.min.x, box.max.y, box.max.z),
    new THREE.Vector3(box.max.x, box.min.y, box.min.z),
    new THREE.Vector3(box.max.x, box.min.y, box.max.z),
    new THREE.Vector3(box.max.x, box.max.y, box.min.z),
    new THREE.Vector3(box.max.x, box.max.y, box.max.z),
  ];

  let viewMinX = Infinity, viewMaxX = -Infinity;
  let viewMinY = Infinity, viewMaxY = -Infinity;
  for (const corner of corners) {
    corner.applyMatrix4(viewMatrix);
    viewMinX = Math.min(viewMinX, corner.x);
    viewMaxX = Math.max(viewMaxX, corner.x);
    viewMinY = Math.min(viewMinY, corner.y);
    viewMaxY = Math.max(viewMaxY, corner.y);
  }

  // Add a small padding so shapes aren't clipped at the edge
  const padding = 1.05;
  const viewWidth = (viewMaxX - viewMinX) * padding;
  const viewHeight = (viewMaxY - viewMinY) * padding;
  const viewCenterX = (viewMinX + viewMaxX) / 2;
  const viewCenterY = (viewMinY + viewMaxY) / 2;

  // Fit frustum: use whichever dimension is the limiting factor
  let frustumHalfW, frustumHalfH;
  if (viewWidth / aspect > viewHeight) {
    frustumHalfW = viewWidth / 2;
    frustumHalfH = frustumHalfW / aspect;
  } else {
    frustumHalfH = viewHeight / 2;
    frustumHalfW = frustumHalfH * aspect;
  }

  // Shift the camera target so the projected center of the scene is in the middle of the viewport
  const right = new THREE.Vector3();
  const up = new THREE.Vector3();
  camera.matrixWorld.extractBasis(right, up, new THREE.Vector3());
  camera.position.add(right.multiplyScalar(viewCenterX));
  camera.position.add(up.multiplyScalar(viewCenterY));

  camera.left = -frustumHalfW;
  camera.right = frustumHalfW;
  camera.top = frustumHalfH;
  camera.bottom = -frustumHalfH;
  camera.updateProjectionMatrix();

  renderOnce();
}

function initThreeJs() {
  const container = containerRef.value;
  if (!container) return;

  renderer = new THREE.WebGLRenderer({antialias: true});
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(container.clientWidth, container.clientHeight);
  container.appendChild(renderer.domElement);

  scene = new THREE.Scene();
  scene.background = new THREE.Color(0xf0f0f0);

  // Orthographic camera — frustum will be set by buildScene to fit content
  camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0.1, 1000);

  buildScene();

  window.addEventListener('resize', onResize);
}

function onResize() {
  if (!containerRef.value || !camera || !renderer) return;
  renderer.setSize(containerRef.value.clientWidth, containerRef.value.clientHeight);
  // Rebuild scene to recompute tight frustum for new aspect ratio
  buildScene();
}

onMounted(() => {
  initThreeJs();
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  if (renderer) {
    renderer.dispose();
    if (containerRef.value && renderer.domElement.parentNode === containerRef.value) {
      containerRef.value.removeChild(renderer.domElement);
    }
  }
});

watch(() => props.shapes, () => {
  buildScene();
}, {deep: true});
</script>

<template>
  <div class="static-render-page">
    <div class="govuk-body govuk-!-margin-bottom-2">
      <strong>3D Shape Viewer</strong> — Static isometric view.
    </div>
    <div class="three-d-layout">
      <div ref="containerRef" class="three-d-container"></div>
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
            <span class="legend-label">
              {{ item.name }}
              <span class="legend-depth">{{ item.depthStart }}m – {{ item.depthEnd }}m</span>
            </span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.static-render-page {
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

.three-d-container {
  flex: 1;
  min-height: 600px;
  border: 1px solid #b1b4b6;
  border-radius: 4px;
  overflow: hidden;
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
  display: flex;
  flex-direction: column;
}

.legend-depth {
  font-size: 12px;
  color: #505a5f;
}
</style>
