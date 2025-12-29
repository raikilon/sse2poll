import 'zone.js';
import 'zone.js/testing';
import { getTestBed } from '@angular/core/testing';
import { BrowserDynamicTestingModule, platformBrowserDynamicTesting } from '@angular/platform-browser-dynamic/testing';

const testBed = getTestBed();

testBed.resetTestEnvironment();
testBed.initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
