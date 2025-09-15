const sass = require('gulp-sass')(require('sass'));
const postcss = require('gulp-postcss');
const gulp = require('gulp');
const sourcemaps = require('gulp-sourcemaps');
const autoprefixer = require('autoprefixer');
const rename = require('gulp-rename');

const rollup = require('rollup');
const babel = require('@rollup/plugin-babel');
const resolve = require('@rollup/plugin-node-resolve');
const commonjs = require('@rollup/plugin-commonjs');
const terser = require('@rollup/plugin-terser');
const vue = require('@vitejs/plugin-vue');
const replace = require('@rollup/plugin-replace');
const css = require('rollup-plugin-css-only');
const node = require('@rollup/plugin-node-resolve');

const sassOptions = {
  outputStyle: 'compressed',
  includePath: 'src/main/resources/scss'
};

function compileSass(exitOnError) {
  const plugins = [
    autoprefixer({
      browsers: ['last 3 versions', '> 0.1%', 'Firefox ESR'],
      grid: true
    })
  ];

  let sassTask = sass(sassOptions);
  if (!exitOnError) {
    //Without an error handler specified, the task will exit on error, which we want for the 'buildAll' task
    sassTask = sassTask.on('error', sass.logError);
  }

  return gulp.src('src/main/resources/scss/*.scss', {base: '.'})
    .pipe(sourcemaps.init())
    .pipe(sassTask)
    .pipe(postcss(plugins))
    .pipe(sourcemaps.write())
    .pipe(rename(path => {
      //E.g. src\main\resources\sass\core -> src\main\resources\public\assets\static\css
      path.dirname = path.dirname.replace(/([\/\\])scss[\/\\]?/, '$1public$1assets$1static$1css');
    }))
    .pipe(gulp.dest('./'));
}

const babelOptions =
  {
    'exclude': 'node_modules/**',
    'presets': [
      ['@babel/preset-env', {
        'useBuiltIns': 'usage',
        'corejs': '3',
        'debug': false,
        'targets': {
          'chrome': 65,
          'firefox': 52,
          'safari': 11,
          'ios': 12,
          'edge': 16
        }
      }]
    ],
    'babelHelpers': 'bundled',
    'plugins': ['@babel/plugin-transform-class-properties'],
  }

gulp.task('rollup-babel', () => rollup.rollup({
    input: './src/main/resources/js/all.js',
    plugins: [
      css({ output: 'gis-alpha-test-bundle.css' }),
      node({ browser: true }),
      replace({
        'process.env.NODE_ENV': JSON.stringify('production'),
      }),
      resolve(),
      vue(),
      commonjs(),
      babel(babelOptions),
      terser(),
    ],
  }).then(bundle => {
    return bundle.write({
      file: './src/main/resources/public/assets/static/js/gis-alpha-test-bundle.js',
      format: 'iife',
      sourcemap: true,
      inlineDynamicImports: true
    })
  })
);

// copy FDS into public/assets
gulp.task('copyFdsResources', () => {
  return gulp.src(['fivium-design-system-core/fds/**/*'])
    .pipe(gulp.dest('src/main/resources/templates/fds'));
});

gulp.task('copyFdsImages', () => {
  return gulp.src(['fivium-design-system-core/fds/static/images/**/*'])
    .pipe(gulp.dest('src/main/resources/public/assets/static/fds/images'));
});

// copy govuk-frontend into public/assets
gulp.task('copyGovukResources', () => {
  return gulp.src(['fivium-design-system-core/node_modules/govuk-frontend/**/*'])
    .pipe(gulp.dest('src/main/resources/public/assets/govuk-frontend'));
});

// copy FDS bundle into public/assets
gulp.task('copyJs', () => {
  return gulp.src(['src/main/resources/templates/fds/static/js/**/*'])
    .pipe(gulp.dest('src/main/resources/public/assets/static/fds/js'));
});

// copy vendor JS into public/assets
gulp.task('copyVendorJs', () => {
  return gulp.src(['src/main/resources/templates/fds/vendor/**/*'])
    .pipe(gulp.dest('src/main/resources/public/assets/static/js/vendor'))
});

// Init all appropriate resources into project's public/assets
gulp.task('initFds', gulp.series(['copyFdsResources', 'copyFdsImages', 'copyGovukResources', 'copyJs', 'copyVendorJs']));

gulp.task('sassCi', gulp.series(['initFds'], () => {
  return compileSass(true);
}));

gulp.task('buildAll', gulp.series(['sassCi', 'rollup-babel']));
