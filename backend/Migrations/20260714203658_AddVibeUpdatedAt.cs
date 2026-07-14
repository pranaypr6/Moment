using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Moment.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddVibeUpdatedAt : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTime>(
                name: "VibeUpdatedAt",
                table: "Users",
                type: "timestamp with time zone",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "VibeUpdatedAt",
                table: "Users");
        }
    }
}
