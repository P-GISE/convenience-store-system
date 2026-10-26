import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('gate',Path(__file__).with_name('check-resolved-gradle.py'))
gate=importlib.util.module_from_spec(spec);spec.loader.exec_module(gate)


def report():
    return {'Results':[{'Type':'gradle','Target':f'{scope}/_.gradle.lockfile',
                        'Packages':[{'Name':'synthetic'}]} for scope in ('runtimeClasspath','testRuntimeClasspath')]}


class ResolvedGateTests(unittest.TestCase):
    def test_both_actual_resolved_scopes_required(self):
        self.assertEqual(len(gate.check(report(),0)),2)

    def test_zero_missing_scope_or_scanner_error_blocks(self):
        for data,status in (({'Results':[]},0),({'Results':report()['Results'][:1]},0),
                            (report(),2),(report(),1)):
            with self.assertRaises(ValueError):gate.check(data,status)

    def test_critical_or_high_in_either_scope_blocks(self):
        for scope in (0,1):
            for severity in ('HIGH','CRITICAL'):
                data=report();data['Results'][scope]['Vulnerabilities']=[{'Severity':severity}]
                with self.assertRaises(ValueError):gate.check(data,1)


if __name__=='__main__':unittest.main()
