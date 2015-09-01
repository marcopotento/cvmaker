(function () {
  'use strict';

  angular
    .module('cvmaker')
    .config(routeConfig);

  function routeConfig($stateProvider, $urlRouterProvider) {
    $stateProvider
      .state('projects', {
        url: '/projects',
        templateUrl: 'app/components/project/list/project.list.html',
        controller: 'ProjectListController as projectList'
      })
      .state('projectEdit', {
        url: '/projects/:projectId/edit',
        resolve: {
          project: ['$stateParams', 'Project', function ($stateParams, Project) {
            if ($stateParams.projectId) {
              return Project.get({projectId: $stateParams.projectId}).$promise;
            }
            else {
              return new Project({technologies: []});
            }
          }]
        },
        templateUrl: 'app/components/project/edit/project.edit.html',
        controller: 'ProjectEditController as projectEdit'
      });

    $urlRouterProvider.otherwise("/projects");
  }

})();
