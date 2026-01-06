export default {
    testEnvironment: 'node',
    transform: {},
    testPathIgnorePatterns: [
        '/node_modules/',
        '/dist/',
        'practice.test.js',           // Ignore specific file
        'integration/',           // Ignore directory
        '.*\\.integration\\.test\\.js$'  // Ignore pattern
    ]


};